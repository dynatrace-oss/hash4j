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
import static com.dynatrace.hash4j.distinctcount.UltraLogLog.Estimator.*;

import java.util.Arrays;

public class UltraLogLogEstimationErrorSimulation {
  public static void main(String[] args) {
    int minP = 3;
    int maxP = 16;
    for (int p = minP; p <= maxP; ++p) {
      doSimulation(
          p,
          "ultraloglog",
          UltraLogLog::create,
          Arrays.asList(
              new EstimationErrorSimulationUtil.EstimatorConfig<UltraLogLog>(
                  (s, m) -> s.getDistinctCountEstimate(),
                  "default",
                  pp -> new UltraLogLogTest().calculateTheoreticalRelativeStandardErrorDefault(pp)),
              new EstimationErrorSimulationUtil.EstimatorConfig<>(
                  (s, m) -> m.getDistinctCountEstimate(),
                  "martingale",
                  pp ->
                      new UltraLogLogTest()
                          .calculateTheoreticalRelativeStandardErrorMartingale(pp)),
              new EstimationErrorSimulationUtil.EstimatorConfig<>(
                  (s, m) -> s.getDistinctCountEstimate(SMALL_RANGE_CORRECTED_1_GRA_ESTIMATOR),
                  "small range corrected 1 GRA",
                  pp -> new UltraLogLogTest().calculateTheoreticalRelativeStandardErrorGRA(pp)),
              new EstimationErrorSimulationUtil.EstimatorConfig<>(
                  (s, m) -> s.getDistinctCountEstimate(SMALL_RANGE_CORRECTED_4_GRA_ESTIMATOR),
                  "small range corrected 4 GRA",
                  pp -> new UltraLogLogTest().calculateTheoreticalRelativeStandardErrorGRA(pp)),
              new EstimationErrorSimulationUtil.EstimatorConfig<>(
                  (s, m) -> s.getDistinctCountEstimate(MAXIMUM_LIKELIHOOD_ESTIMATOR),
                  "maximum likelihood",
                  pp -> new UltraLogLogTest().calculateTheoreticalRelativeStandardErrorML(pp))));
    }
  }
}
