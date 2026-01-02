/*
 * Copyright 2025-2026 Dynatrace LLC
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
package com.dynatrace.hash4j.consistent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class BinaryMatrixTest {

  @Test
  void testMatrix() {
    assertThat(BinaryMatrix.identity(3)).hasToString("[[1, 0, 0], [0, 1, 0], [0, 0, 1]]");
    assertThat(BinaryMatrix.lsh(3)).hasToString("[[0, 1, 0], [0, 0, 1], [0, 0, 0]]");
    assertThat(BinaryMatrix.rsh(3)).hasToString("[[0, 0, 0], [1, 0, 0], [0, 1, 0]]");
    assertThat(BinaryMatrix.pow(BinaryMatrix.lsh(3), 2))
        .hasToString("[[0, 0, 1], [0, 0, 0], [0, 0, 0]]");
    assertThat(BinaryMatrix.pow(BinaryMatrix.rsh(3), 2))
        .hasToString("[[0, 0, 0], [0, 0, 0], [1, 0, 0]]");
    assertThat(BinaryMatrix.rsh(3)).hasToString("[[0, 0, 0], [1, 0, 0], [0, 1, 0]]");
    assertThat(BinaryMatrix.equals(BinaryMatrix.identity(3), BinaryMatrix.identity(3))).isTrue();
    assertThat(BinaryMatrix.equals(BinaryMatrix.identity(3), BinaryMatrix.identity(3).copy()))
        .isTrue();
    assertThat(
            BinaryMatrix.equals(
                BinaryMatrix.pow(BinaryMatrix.identity(3), 2), BinaryMatrix.identity(3)))
        .isTrue();
    assertThat(
            BinaryMatrix.equals(
                BinaryMatrix.add(BinaryMatrix.zero(3), BinaryMatrix.zero(3)), BinaryMatrix.zero(3)))
        .isTrue();
    assertThat(
            BinaryMatrix.equals(
                BinaryMatrix.add(BinaryMatrix.identity(3), BinaryMatrix.zero(3)),
                BinaryMatrix.identity(3)))
        .isTrue();
    assertThat(
            BinaryMatrix.equals(
                BinaryMatrix.add(BinaryMatrix.identity(3), BinaryMatrix.identity(3)),
                BinaryMatrix.zero(3)))
        .isTrue();
    assertThat(
            BinaryMatrix.equals(
                BinaryMatrix.pow(BinaryMatrix.identity(3), 3), BinaryMatrix.identity(3)))
        .isTrue();
    assertThat(BinaryMatrix.equals(BinaryMatrix.pow(BinaryMatrix.lsh(3), 3), BinaryMatrix.zero(3)))
        .isTrue();
    assertThat(BinaryMatrix.equals(BinaryMatrix.pow(BinaryMatrix.rsh(3), 3), BinaryMatrix.zero(3)))
        .isTrue();
    assertThat(BinaryMatrix.identity(3).isIdentity()).isTrue();
    assertThat(BinaryMatrix.lsh(3).isIdentity()).isFalse();
    assertThat(BinaryMatrix.rsh(3).isIdentity()).isFalse();
  }
}
