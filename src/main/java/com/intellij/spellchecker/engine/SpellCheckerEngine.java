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

import com.intellij.spellchecker.dictionary.Loader;
import jakarta.annotation.Nonnull;

import java.util.List;


public interface SpellCheckerEngine {


  void loadDictionary(@Nonnull Loader loader);

  Transformation getTransformation();

  boolean isCorrect(@Nonnull String word);


  @Nonnull
  List<String> getSuggestions(@Nonnull String word, int threshold, int quality);

  @Nonnull
  List<String> getVariants(@Nonnull String prefix);


  void reset();

  boolean isDictionaryLoad(@Nonnull String name);

  void removeDictionary(@Nonnull String name);
}
