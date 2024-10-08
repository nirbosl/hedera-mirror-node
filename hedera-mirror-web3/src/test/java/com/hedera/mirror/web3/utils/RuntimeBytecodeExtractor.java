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

package com.hedera.mirror.web3.utils;

import static com.hedera.mirror.web3.validation.HexValidator.HEX_PREFIX;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class for extracting runtime bytecode from init bytecode of a smart contract.
 * <p>
 * Smart contracts have init bytecode (constructor bytecode) and runtime bytecode (the code
 * executed when the contract is called). This class helps in extracting the runtime bytecode from the
 * given init bytecode by searching for specific patterns.
 * </p>
 */
@UtilityClass
public class RuntimeBytecodeExtractor {

    private static final String CODECOPY = "39";
    private static final String RUNTIME_CODE_PREFIX =
            "6080"; // The pattern to find the start of the runtime code in the init bytecode

    public static String extractRuntimeBytecode(String initBytecode) {
        // Check if the bytecode starts with "0x" and remove it if necessary
        if (initBytecode.startsWith(HEX_PREFIX)) {
            initBytecode = initBytecode.substring(2);
        }

        String runtimeBytecode = getRuntimeBytecode(initBytecode);

        return HEX_PREFIX + runtimeBytecode; // Append "0x" prefix and return
    }

    @NotNull
    private static String getRuntimeBytecode(final String initBytecode) {
        // Find the first occurrence of "CODECOPY" (39)
        int codeCopyIndex = initBytecode.indexOf(CODECOPY);

        if (codeCopyIndex == -1) {
            throw new RuntimeException("CODECOPY instruction (39) not found in init bytecode.");
        }

        // Find the first occurrence of "6080" after the "CODECOPY"
        int runtimeCodePrefixIndex = initBytecode.indexOf(RUNTIME_CODE_PREFIX, codeCopyIndex);

        if (runtimeCodePrefixIndex == -1) {
            throw new RuntimeException("Runtime code prefix (6080) not found after CODECOPY.");
        }

        // Extract the runtime bytecode starting from the runtimeCodePrefixIndex
        return initBytecode.substring(runtimeCodePrefixIndex);
    }
}
