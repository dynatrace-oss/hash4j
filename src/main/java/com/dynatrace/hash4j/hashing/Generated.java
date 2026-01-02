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
package com.dynatrace.hash4j.hashing;

import java.lang.annotation.RetentionPolicy;

/** Annotation to exclude from code coverage computation. */
@java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
@interface Generated {
  /**
   * Returns the reason for the exclusion.
   *
   * @return the reason for the exclusion.
   */
  String reason();
}
