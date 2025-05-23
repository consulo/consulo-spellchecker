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
package com.intellij.spellchecker.state;

import com.intellij.spellchecker.dictionary.EditableDictionary;
import com.intellij.spellchecker.dictionary.EditableDictionaryLoader;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import java.util.Set;
import java.util.function.Consumer;

public class StateLoader implements EditableDictionaryLoader {

  private final Project project;
  private EditableDictionary dictionary;

  public StateLoader(Project project) {
    this.project = project;
  }


  public void load(@Nonnull Consumer<String> consumer) {
    AggregatedDictionaryState state = project.getInstance(AggregatedDictionaryState.class);
    state.setProject(project);
    state.loadState();
    dictionary = state.getDictionary();
    if (dictionary == null) {
      return;
    }
    final Set<String> storedWords = dictionary.getWords();
    if (storedWords != null) {
      for (String word : storedWords) {
        consumer.accept(word);
      }
    }

  }

  public EditableDictionary getDictionary() {
    return dictionary;
  }

  public String getName() {
    return (dictionary != null ? dictionary.getName() : "");
  }
}


