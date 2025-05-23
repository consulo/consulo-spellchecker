/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.spellchecker.engine;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"ALL"})
public class Transformation {
  
  @Nullable
  public  String transform(@Nullable String word) {
    if (word == null || word.trim().length() < 3) {
      return null;
    }
    return word.trim().toLowerCase();
  }

  @Nullable
  public  Set<String> transform(@Nullable Collection<String> words) {
    if (words == null || words.isEmpty()) {
      return null;
    }
    Set<String> result = new HashSet<String>();
    for (String word : words) {
      String transformed = transform(word);
      if (transformed != null) {
        result.add(transformed);
      }
    }
    return result;
  }
}
