/*
 * Copyright 2026 Dynatrace LLC
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

import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;

final class PerformanceTestUtil {

  private PerformanceTestUtil() {}

  static final Funnel<String> GUAVA_CHARS_FUNNEL = (s, sink) -> sink.putUnencodedChars(s);
  static final Funnel<String> GUAVA_CHARS_UTF8_FUNNEL =
      (s, sink) -> sink.putString(s, StandardCharsets.UTF_8);
  static final Funnel<byte[]> GUAVA_BYTES_FUNNEL = (s, sink) -> sink.putBytes(s);

  static final HashFunnel<CharSequence> HASH4J_CHARS_FUNNEL = (s, sink) -> sink.putChars(s);
  static final HashFunnel<CharSequence> HASH4J_CHARS_UTF8_FUNNEL =
      (s, sink) -> sink.putCharsUTF8(s);
  static final HashFunnel<byte[]> HASH4J_BYTES_FUNNEL = (s, sink) -> sink.putBytes(s);
}
