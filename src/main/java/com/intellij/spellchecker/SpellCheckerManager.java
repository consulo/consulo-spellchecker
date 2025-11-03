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

import com.intellij.spellchecker.dictionary.EditableDictionary;
import com.intellij.spellchecker.dictionary.Loader;
import com.intellij.spellchecker.engine.SpellCheckerEngine;
import com.intellij.spellchecker.engine.SpellCheckerFactory;
import com.intellij.spellchecker.engine.SuggestionProvider;
import com.intellij.spellchecker.settings.SpellCheckerSettings;
import com.intellij.spellchecker.state.StateLoader;
import com.intellij.spellchecker.util.SPFileUtil;
import com.intellij.spellchecker.util.Strings;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.spellchecker.editor.SpellcheckerSeverities;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.InputStream;
import java.util.*;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class SpellCheckerManager {
    private static final Logger LOG = Logger.getInstance(SpellCheckerManager.class);

    private static final int MAX_SUGGESTIONS_THRESHOLD = 5;
    private static final int MAX_METRICS = 1;

    private final Project myProject;
    private SpellCheckerEngine mySpellChecker;
    private EditableDictionary myUserDictionary;

    @Nonnull
    private final SuggestionProvider suggestionProvider = new BaseSuggestionProvider(this);

    private final SpellCheckerSettings settings;

    @Deprecated
    public static SpellCheckerManager getInstance(@Nonnull Project project) {
        return project.getInstance(SpellCheckerManager.class);
    }

    @Inject
    public SpellCheckerManager(@Nonnull Project project, SpellCheckerSettings settings) {
        this.myProject = project;
        this.settings = settings;
        fullConfigurationReload();
    }

    public void fullConfigurationReload() {
        mySpellChecker = SpellCheckerFactory.create(myProject);
        fillEngineDictionary();
    }

    public void updateBundledDictionaries(List<String> removedDictionaries) {
        myProject.getApplication().getExtensionPoint(BundledDictionaryProvider.class).forEach(provider -> {
            for (String dictionary : provider.getBundledDictionaries()) {
                boolean dictionaryShouldBeLoad = settings == null || !settings.getBundledDisabledDictionariesPaths().contains(dictionary);
                boolean dictionaryIsLoad = mySpellChecker.isDictionaryLoad(dictionary);
                if (dictionaryIsLoad && !dictionaryShouldBeLoad) {
                    mySpellChecker.removeDictionary(dictionary);
                }
                else if (!dictionaryIsLoad && dictionaryShouldBeLoad) {
                    Class<? extends BundledDictionaryProvider> loaderClass = provider.getClass();
                    InputStream stream = loaderClass.getResourceAsStream(dictionary);
                    if (stream != null) {
                        mySpellChecker.loadDictionary(new StreamLoader(stream, dictionary));
                    }
                    else {
                        LOG.warn("Couldn't load dictionary '" + dictionary + "' with loader '" + loaderClass + "'");
                    }
                }
            }
        });
        if (settings != null && settings.getDictionaryFoldersPaths() != null) {
            Set<String> disabledDictionaries = settings.getDisabledDictionariesPaths();
            for (String folder : settings.getDictionaryFoldersPaths()) {
                SPFileUtil.processFilesRecursively(
                    folder,
                    s -> {
                        boolean dictionaryShouldBeLoad = !disabledDictionaries.contains(s);
                        boolean dictionaryIsLoad = mySpellChecker.isDictionaryLoad(s);
                        if (dictionaryIsLoad && !dictionaryShouldBeLoad) {
                            mySpellChecker.removeDictionary(s);
                        }
                        else if (!dictionaryIsLoad && dictionaryShouldBeLoad) {
                            mySpellChecker.loadDictionary(new FileLoader(s, s));
                        }
                    }
                );
            }
        }

        if (removedDictionaries != null && !removedDictionaries.isEmpty()) {
            for (String name : removedDictionaries) {
                mySpellChecker.removeDictionary(name);
            }
        }

        restartInspections();
    }

    public Project getProject() {
        return myProject;
    }

    public EditableDictionary getUserDictionary() {
        return myUserDictionary;
    }

    private void fillEngineDictionary() {
        mySpellChecker.reset();
        StateLoader stateLoader = new StateLoader(myProject);
        stateLoader.load(s -> {
            //do nothing - in this loader we don't worry about word list itself - the whole dictionary will be restored
        });
        List<Loader> loaders = new ArrayList<>();
        // Load bundled dictionaries from corresponding jars
        myProject.getApplication().getExtensionPoint(BundledDictionaryProvider.class).forEach(provider -> {
            for (String dictionary : provider.getBundledDictionaries()) {
                if (settings == null || !settings.getBundledDisabledDictionariesPaths().contains(dictionary)) {
                    Class<? extends BundledDictionaryProvider> loaderClass = provider.getClass();
                    InputStream stream = loaderClass.getResourceAsStream(dictionary);
                    if (stream != null) {
                        loaders.add(new StreamLoader(stream, dictionary));
                    }
                    else {
                        LOG.warn("Couldn't load dictionary '" + dictionary + "' with loader '" + loaderClass + "'");
                    }
                }
            }
        });
        if (settings != null && settings.getDictionaryFoldersPaths() != null) {
            Set<String> disabledDictionaries = settings.getDisabledDictionariesPaths();
            for (String folder : settings.getDictionaryFoldersPaths()) {
                SPFileUtil.processFilesRecursively(folder, s -> {
                    if (!disabledDictionaries.contains(s)) {
                        loaders.add(new FileLoader(s, s));
                    }
                });
            }
        }
        loaders.add(stateLoader);
        for (Loader loader : loaders) {
            mySpellChecker.loadDictionary(loader);
        }
        myUserDictionary = stateLoader.getDictionary();

    }

    public boolean hasProblem(@Nonnull String word) {
        return !mySpellChecker.isCorrect(word);
    }

    @RequiredWriteAction
    public void acceptWordAsCorrect(@Nonnull String word, Project project) {
        String transformed = mySpellChecker.getTransformation().transform(word);
        if (transformed != null) {
            myUserDictionary.addToDictionary(transformed);
            PsiModificationTracker modificationTracker = PsiManager.getInstance(project).getModificationTracker();
            modificationTracker.incCounter();
        }
    }

    public void updateUserDictionary(@Nullable Collection<String> words) {
        myUserDictionary.replaceAll(words);
        restartInspections();
    }

    @Nonnull
    public static List<String> getBundledDictionaries() {
        List<String> dictionaries = new ArrayList<>();
        Application.get().getExtensionPoint(BundledDictionaryProvider.class)
            .forEach(provider -> ContainerUtil.addAll(dictionaries, provider.getBundledDictionaries()));
        return dictionaries;
    }

    @Nonnull
    public static HighlightDisplayLevel getHighlightDisplayLevel() {
        return HighlightDisplayLevel.find(SpellcheckerSeverities.TYPO);
    }

    @Nonnull
    public List<String> getSuggestions(@Nonnull String text) {
        return suggestionProvider.getSuggestions(text);
    }

    @Nonnull
    protected List<String> getRawSuggestions(@Nonnull String word) {
        if (!mySpellChecker.isCorrect(word)) {
            List<String> suggestions = mySpellChecker.getSuggestions(word, MAX_SUGGESTIONS_THRESHOLD, MAX_METRICS);
            if (!suggestions.isEmpty()) {
                boolean capitalized = Strings.isCapitalized(word);
                boolean upperCases = Strings.isUpperCase(word);
                if (capitalized) {
                    Strings.capitalize(suggestions);
                }
                else if (upperCases) {
                    Strings.upperCase(suggestions);
                }
            }
            return new ArrayList<>(new LinkedHashSet<>(suggestions));
        }
        return Collections.emptyList();
    }

    public static void restartInspections() {
        Application.get().invokeLater(() -> {
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : projects) {
                if (project.isInitialized() && project.isOpen() && !project.isDefault()) {
                    DaemonCodeAnalyzer.getInstance(project).restart();
                }
            }
        });
    }
}
