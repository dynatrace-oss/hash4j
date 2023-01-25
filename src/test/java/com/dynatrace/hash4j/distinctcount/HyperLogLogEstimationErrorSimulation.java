/*
 * Copyright 2022-2023 Dynatrace LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dynatrace.hash4j.distinctcount;

import static com.dynatrace.hash4j.distinctcount.EstimationErrorSimulationUtil.doSimulation;
import static com.dynatrace.hash4j.distinctcount.HyperLogLog.*;

import java.util.Arrays;

public class HyperLogLogEstimationErrorSimulation {
  public static void main(String[] args) {
    int p = Integer.parseInt(args[0]);
    String outputFile = args[1];
    doSimulation(
        p,
        "hyperloglog",
        HyperLogLog::create,
        Arrays.asList(
            new EstimationErrorSimulationUtil.EstimatorConfig<HyperLogLog>(
                (s, m) -> s.getDistinctCountEstimate(),
                "default",
                pp -> new HyperLogLogTest().calculateTheoreticalRelativeStandardErrorDefault(pp)),
            new EstimationErrorSimulationUtil.EstimatorConfig<>(
                (s, m) -> m.getDistinctCountEstimate(),
                "martingale",
                pp ->
                    new HyperLogLogTest().calculateTheoreticalRelativeStandardErrorMartingale(pp)),
            new EstimationErrorSimulationUtil.EstimatorConfig<>(
                (s, m) -> s.getDistinctCountEstimate(MAXIMUM_LIKELIHOOD_ESTIMATOR),
                "maximum likelihood",
                pp -> new HyperLogLogTest().calculateTheoreticalRelativeStandardErrorML(pp)),
            new EstimationErrorSimulationUtil.EstimatorConfig<>(
                (s, m) -> s.getDistinctCountEstimate(SMALL_RANGE_CORRECTED_RAW_ESTIMATOR),
                "small range corrected raw",
                pp -> new HyperLogLogTest().calculateTheoreticalRelativeStandardErrorRaw(pp)),
            new EstimationErrorSimulationUtil.EstimatorConfig<>(
                (s, m) -> s.getDistinctCountEstimate(CORRECTED_RAW_ESTIMATOR),
                "corrected raw",
                pp -> new HyperLogLogTest().calculateTheoreticalRelativeStandardErrorRaw(pp))),
        outputFile);
  }
}
