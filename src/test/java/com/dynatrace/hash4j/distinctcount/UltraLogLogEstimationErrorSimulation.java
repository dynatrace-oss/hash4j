/*
 * Copyright 2022-2026 Dynatrace LLC
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
import static com.dynatrace.hash4j.distinctcount.UltraLogLog.MAXIMUM_LIKELIHOOD_ESTIMATOR;
import static com.dynatrace.hash4j.distinctcount.UltraLogLog.OPTIMAL_FGRA_ESTIMATOR;
import static com.dynatrace.hash4j.distinctcount.UltraLogLog.OptimalFGRAEstimator;

import java.util.Arrays;

public class UltraLogLogEstimationErrorSimulation {
  public static void main(String[] args) {
    int p = Integer.parseInt(args[0]);
    String outputFile = args[1];
    doSimulation(
        p,
        "ultraloglog",
        UltraLogLog::create,
        Arrays.asList(
            new EstimationErrorSimulationUtil.EstimatorConfig<>(
                (s, m) -> s.getDistinctCountEstimate(),
                "default",
                pp -> new UltraLogLogTest().calculateTheoreticalRelativeStandardErrorDefault(pp)),
            new EstimationErrorSimulationUtil.EstimatorConfig<>(
                (s, m) -> m.getDistinctCountEstimate(),
                "martingale",
                pp ->
                    new UltraLogLogTest().calculateTheoreticalRelativeStandardErrorMartingale(pp)),
            new EstimationErrorSimulationUtil.EstimatorConfig<>(
                (s, m) -> s.getDistinctCountEstimate(OPTIMAL_FGRA_ESTIMATOR),
                "optimal FGRA",
                OptimalFGRAEstimator::calculateTheoreticalRelativeStandardError),
            new EstimationErrorSimulationUtil.EstimatorConfig<>(
                (s, m) -> s.getDistinctCountEstimate(MAXIMUM_LIKELIHOOD_ESTIMATOR),
                "maximum likelihood",
                pp -> new UltraLogLogTest().calculateTheoreticalRelativeStandardErrorML(pp))),
        outputFile,
        TestUtils.getHashGenerators1(p));
  }
}
