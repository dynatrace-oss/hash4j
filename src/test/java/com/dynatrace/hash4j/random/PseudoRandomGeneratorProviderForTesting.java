/*
 * Copyright 2023-2025 Dynatrace LLC
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
package com.dynatrace.hash4j.random;

public class PseudoRandomGeneratorProviderForTesting implements PseudoRandomGeneratorProvider {

  private long longValue;
  private int intValue;

  private int uniformIntValue;

  private double doubleValue;

  private double exponentialValue;

  public long getLongValue() {
    return longValue;
  }

  public void setLongValue(long longValue) {
    this.longValue = longValue;
  }

  public int getIntValue() {
    return intValue;
  }

  public void setIntValue(int intValue) {
    this.intValue = intValue;
  }

  public int getUniformIntValue() {
    return uniformIntValue;
  }

  public void setUniformIntValue(int uniformIntValue) {
    this.uniformIntValue = uniformIntValue;
  }

  public double getDoubleValue() {
    return doubleValue;
  }

  public void setDoubleValue(double doubleValue) {
    this.doubleValue = doubleValue;
  }

  public double getExponentialValue() {
    return exponentialValue;
  }

  public void setExponentialValue(double exponentialValue) {
    this.exponentialValue = exponentialValue;
  }

  @Override
  public PseudoRandomGenerator create() {
    return new PseudoRandomGenerator() {
      @Override
      public long nextLong() {
        return longValue;
      }

      @Override
      public int nextInt() {
        return intValue;
      }

      @Override
      public int uniformInt(int exclusiveBound) {
        return uniformIntValue;
      }

      @Override
      public PseudoRandomGenerator reset(long seed) {
        return this;
      }

      @Override
      public double nextDouble() {
        return doubleValue;
      }

      @Override
      public double nextExponential() {
        return exponentialValue;
      }
    };
  }
}
