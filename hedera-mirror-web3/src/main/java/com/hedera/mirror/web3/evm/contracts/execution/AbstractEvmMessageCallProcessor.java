/*
 * Copyright (C) 2024 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hedera.mirror.web3.evm.contracts.execution;

import static com.hedera.services.stream.proto.ContractActionType.PRECOMPILE;
import static com.hedera.services.stream.proto.ContractActionType.SYSTEM;
import static org.apache.tuweni.bytes.Bytes.EMPTY;
import static org.hyperledger.besu.evm.frame.ExceptionalHaltReason.DefaultExceptionalHaltReason.INSUFFICIENT_GAS;
import static org.hyperledger.besu.evm.frame.MessageFrame.State.COMPLETED_SUCCESS;
import static org.hyperledger.besu.evm.frame.MessageFrame.State.EXCEPTIONAL_HALT;
import static org.hyperledger.besu.evm.frame.MessageFrame.State.REVERT;

import com.hedera.node.app.service.evm.contracts.execution.HederaEvmMessageCallProcessor;
import com.hedera.node.app.service.evm.store.contracts.AbstractLedgerEvmWorldUpdater;
import com.hedera.node.app.service.evm.store.contracts.precompile.EvmHTSPrecompiledContract;
import com.hedera.node.app.service.mono.contracts.execution.traceability.HederaOperationTracer;
import com.swirlds.base.utility.Pair;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.precompile.PrecompileContractRegistry;
import org.hyperledger.besu.evm.precompile.PrecompiledContract;
import org.hyperledger.besu.evm.tracing.OperationTracer;

public abstract class AbstractEvmMessageCallProcessor extends HederaEvmMessageCallProcessor {

    private final Predicate<Address> isNativePrecompileCheck;

    protected AbstractEvmMessageCallProcessor(
            final EVM evm,
            final PrecompileContractRegistry precompiles,
            final Map<String, PrecompiledContract> hederaPrecompileList) {
        super(evm, precompiles, hederaPrecompileList);
        isNativePrecompileCheck = addr -> precompiles.get(addr) != null;
    }

    @Override
    public void start(final MessageFrame frame, final OperationTracer operationTracer) {
        super.start(frame, operationTracer);

        // potential precompile execution will be done after super.start(), so trace results here
        final Address contractAddress = frame.getContractAddress();
        final boolean isNativePrecompile = isNativePrecompileCheck.test(contractAddress);
        final boolean isHederaPrecompile = hederaPrecompiles.containsKey(contractAddress);
        if ((isNativePrecompile || isHederaPrecompile)
                && operationTracer instanceof HederaOperationTracer hederaOperationTracer) {
            hederaOperationTracer.tracePrecompileResult(frame, isHederaPrecompile ? SYSTEM : PRECOMPILE);
        }
    }

    @Override
    protected void executeHederaPrecompile(
            PrecompiledContract contract, MessageFrame frame, OperationTracer operationTracer) {
        Pair<Long, Bytes> costAndResult = calculatePrecompileGasAndOutput(contract, frame);

        Long gasRequirement = costAndResult.left();
        Bytes output = costAndResult.right();

        traceAndHandleExecutionResult(frame, operationTracer, gasRequirement, output);
    }

    private Pair<Long, Bytes> calculatePrecompileGasAndOutput(PrecompiledContract contract, MessageFrame frame) {
        Bytes output = EMPTY;
        Long gasRequirement = 0L;

        if (contract instanceof EvmHTSPrecompiledContract htsPrecompile) {
            var updater = (AbstractLedgerEvmWorldUpdater) frame.getWorldUpdater();
            final var costedResult = htsPrecompile.computeCosted(
                    frame.getInputData(),
                    frame,
                    (now, minimumTinybarCost) -> minimumTinybarCost,
                    updater.tokenAccessor());
            output = costedResult.getValue();
            gasRequirement = costedResult.getKey();
        }

        if (!"HTS".equals(contract.getName()) && !"EvmHTS".equals(contract.getName())) {
            output = contract.computePrecompile(frame.getInputData(), frame).getOutput();
            gasRequirement = contract.gasRequirement(frame.getInputData());
        }

        return Pair.of(gasRequirement, output);
    }

    private void traceAndHandleExecutionResult(
            MessageFrame frame, OperationTracer operationTracer, Long gasRequirement, Bytes output) {
        operationTracer.tracePrecompileCall(frame, gasRequirement, output);
        if (frame.getState() == REVERT) {
            return;
        }

        if (frame.getRemainingGas() < gasRequirement) {
            frame.decrementRemainingGas(frame.getRemainingGas());
            frame.setExceptionalHaltReason(Optional.of(INSUFFICIENT_GAS));
            frame.setState(EXCEPTIONAL_HALT);
        } else if (output != null) {
            frame.decrementRemainingGas(gasRequirement);
            frame.setOutputData(output);
            frame.setState(COMPLETED_SUCCESS);
        } else {
            frame.setState(EXCEPTIONAL_HALT);
        }
    }
}
