// SPDX-License-Identifier: Apache-2.0

package com.hedera.mirror.test.e2e.acceptance.config;

import jakarta.inject.Named;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Named
@ConfigurationProperties(prefix = "hedera.mirror.test.acceptance.feature")
@Data
@RequiredArgsConstructor
@Validated
public class FeatureProperties {

    @Min(1)
    @Max(5_000_000)
    private long maxContractFunctionGas = 3_000_000;

    private boolean sidecars = false;
}
