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
package com.intellij.spellchecker;

import com.intellij.spellchecker.engine.SpellCheckerEngine;
import com.intellij.spellchecker.engine.SpellCheckerFactory;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * English dictionaries of the Simple Spellchecker engine. The service is created, and the dictionaries are loaded, on the first
 * check only, so nothing is loaded while another engine is selected.
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class SimpleSpellcheckerManager {
    private static final int MAX_SUGGESTIONS_THRESHOLD = 5;
    private static final int MAX_METRICS = 1;

    private final Project myProject;
    private final Application myApplication;

    private volatile @Nullable SpellCheckerEngine mySpellChecker;

    @Inject
    public SimpleSpellcheckerManager(Project project, Application application) {
        myProject = project;
        myApplication = application;
    }

    public static SimpleSpellcheckerManager getInstance(@Nonnull Project project) {
        return project.getInstance(SimpleSpellcheckerManager.class);
    }

    @Nonnull
    private SpellCheckerEngine getSpellChecker() {
        SpellCheckerEngine spellChecker = mySpellChecker;
        if (spellChecker != null) {
            return spellChecker;
        }
        synchronized (this) {
            spellChecker = mySpellChecker;
            if (spellChecker == null) {
                spellChecker = SpellCheckerFactory.create(myProject);
                SpellCheckerEngine target = spellChecker;
                myApplication.getExtensionPoint(SimpleDictionaryProvider.class)
                    .forEach(provider -> provider.loadDictionaries((name, stream) -> target.loadDictionary(new StreamLoader(stream, name))));
                mySpellChecker = spellChecker;
            }
            return spellChecker;
        }
    }

    public boolean hasProblem(@Nonnull String word) {
        return !getSpellChecker().isCorrect(word);
    }

    @Nonnull
    public List<String> getSuggestions(@Nonnull String word) {
        SpellCheckerEngine spellChecker = getSpellChecker();
        if (spellChecker.isCorrect(word)) {
            return List.of();
        }
        List<String> suggestions = spellChecker.getSuggestions(word, MAX_SUGGESTIONS_THRESHOLD, MAX_METRICS);
        return new ArrayList<>(new LinkedHashSet<>(suggestions));
    }
}
