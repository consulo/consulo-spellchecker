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
    private final Project myProject;
    private EditableDictionary myDictionary;

    public StateLoader(Project project) {
        this.myProject = project;
    }

    @Override
    public void load(@Nonnull Consumer<String> consumer) {
        AggregatedDictionaryState state = myProject.getInstance(AggregatedDictionaryState.class);
        state.setProject(myProject);
        state.loadState();
        myDictionary = state.getDictionary();
        if (myDictionary == null) {
            return;
        }
        Set<String> storedWords = myDictionary.getWords();
        if (storedWords != null) {
            for (String word : storedWords) {
                consumer.accept(word);
            }
        }
    }

    @Override
    public EditableDictionary getDictionary() {
        return myDictionary;
    }

    @Override
    public String getName() {
        return (myDictionary != null ? myDictionary.getName() : "");
    }
}


