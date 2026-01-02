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
package com.dynatrace.hash4j.internal;

import java.util.Locale;

/**
 * Utility class for preconditions.
 *
 * <p>As an internal package it is not intended for general use.
 */
public final class Preconditions {

  private Preconditions() {}

  /**
   * Throws an {@link IllegalArgumentException} if the given expression evaluates to {@code false}.
   *
   * @param expression an expression
   * @throws IllegalArgumentException if the given expression evaluates to {@code false}
   */
  public static void checkArgument(boolean expression) {
    if (!expression) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Throws an {@link IllegalStateException} if the given expression evaluates to {@code false}.
   *
   * @param expression an expression
   * @throws IllegalStateException if the given expression evaluates to {@code false}
   */
  public static void checkState(boolean expression) {
    if (!expression) {
      throw new IllegalStateException();
    }
  }

  /**
   * Throws an {@link IllegalArgumentException} if the given expression evaluates to {@code false}.
   *
   * @param expression an expression
   * @param errorMessage an error message
   * @throws IllegalArgumentException if the given expression evaluates to {@code false}
   */
  public static void checkArgument(boolean expression, String errorMessage) {
    if (!expression) {
      throw new IllegalArgumentException(errorMessage);
    }
  }

  /**
   * Throws an {@link IllegalArgumentException} if the given expression evaluates to {@code false}.
   *
   * @param expression an expression
   * @param errorMessageFormatString an error message format string with a single %s place holder
   * @param value a long value
   * @throws IllegalArgumentException if the given expression evaluates to {@code false}
   */
  public static void checkArgument(
      boolean expression, String errorMessageFormatString, long value) {
    if (!expression) {
      throw new IllegalArgumentException(
          String.format(Locale.ROOT, errorMessageFormatString, Long.valueOf(value)));
    }
  }
}
