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
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.ide.ServiceManager;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.spellchecker.editor.SpellcheckerSeverities;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.collection.ContainerUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class SpellCheckerManager
{
	private static final Logger LOG = Logger.getInstance(SpellCheckerManager.class);

	private static final int MAX_SUGGESTIONS_THRESHOLD = 5;
	private static final int MAX_METRICS = 1;

	private final Project project;

	private SpellCheckerEngine spellChecker;

	private EditableDictionary userDictionary;


	@Nonnull
	private final SuggestionProvider suggestionProvider = new BaseSuggestionProvider(this);

	private final SpellCheckerSettings settings;

	public static SpellCheckerManager getInstance(Project project)
	{
		return ServiceManager.getService(project, SpellCheckerManager.class);
	}

	@Inject
	public SpellCheckerManager(Project project, SpellCheckerSettings settings)
	{
		this.project = project;
		this.settings = settings;
		fullConfigurationReload();
	}

	public void fullConfigurationReload()
	{
		spellChecker = SpellCheckerFactory.create(project);
		fillEngineDictionary();
	}


	public void updateBundledDictionaries(final List<String> removedDictionaries)
	{
		for(BundledDictionaryProvider provider : BundledDictionaryProvider.EP_NAME.getExtensionList())
		{
			for(String dictionary : provider.getBundledDictionaries())
			{
				boolean dictionaryShouldBeLoad = settings == null || !settings.getBundledDisabledDictionariesPaths().contains(dictionary);
				boolean dictionaryIsLoad = spellChecker.isDictionaryLoad(dictionary);
				if(dictionaryIsLoad && !dictionaryShouldBeLoad)
				{
					spellChecker.removeDictionary(dictionary);
				}
				else if(!dictionaryIsLoad && dictionaryShouldBeLoad)
				{
					final Class<? extends BundledDictionaryProvider> loaderClass = provider.getClass();
					final InputStream stream = loaderClass.getResourceAsStream(dictionary);
					if(stream != null)
					{
						spellChecker.loadDictionary(new StreamLoader(stream, dictionary));
					}
					else
					{
						LOG.warn("Couldn't load dictionary '" + dictionary + "' with loader '" + loaderClass + "'");
					}
				}
			}
		}
		if(settings != null && settings.getDictionaryFoldersPaths() != null)
		{
			final Set<String> disabledDictionaries = settings.getDisabledDictionariesPaths();
			for(String folder : settings.getDictionaryFoldersPaths())
			{
				SPFileUtil.processFilesRecursively(folder, new Consumer<String>()
				{
					@Override
					public void accept(final String s)
					{
						boolean dictionaryShouldBeLoad = !disabledDictionaries.contains(s);
						boolean dictionaryIsLoad = spellChecker.isDictionaryLoad(s);
						if(dictionaryIsLoad && !dictionaryShouldBeLoad)
						{
							spellChecker.removeDictionary(s);
						}
						else if(!dictionaryIsLoad && dictionaryShouldBeLoad)
						{
							spellChecker.loadDictionary(new FileLoader(s, s));
						}

					}
				});

			}
		}

		if(removedDictionaries != null && !removedDictionaries.isEmpty())
		{
			for(final String name : removedDictionaries)
			{
				spellChecker.removeDictionary(name);
			}
		}

		restartInspections();
	}

	public Project getProject()
	{
		return project;
	}

	public EditableDictionary getUserDictionary()
	{
		return userDictionary;
	}

	private void fillEngineDictionary()
	{
		spellChecker.reset();
		final StateLoader stateLoader = new StateLoader(project);
		stateLoader.load(new Consumer<String>()
		{
			@Override
			public void accept(String s)
			{
				//do nothing - in this loader we don't worry about word list itself - the whole dictionary will be restored
			}
		});
		final List<Loader> loaders = new ArrayList<Loader>();
		// Load bundled dictionaries from corresponding jars
		for(BundledDictionaryProvider provider : BundledDictionaryProvider.EP_NAME.getExtensionList())
		{
			for(String dictionary : provider.getBundledDictionaries())
			{
				if(settings == null || !settings.getBundledDisabledDictionariesPaths().contains(dictionary))
				{
					final Class<? extends BundledDictionaryProvider> loaderClass = provider.getClass();
					final InputStream stream = loaderClass.getResourceAsStream(dictionary);
					if(stream != null)
					{
						loaders.add(new StreamLoader(stream, dictionary));
					}
					else
					{
						LOG.warn("Couldn't load dictionary '" + dictionary + "' with loader '" + loaderClass + "'");
					}
				}
			}
		}
		if(settings != null && settings.getDictionaryFoldersPaths() != null)
		{
			final Set<String> disabledDictionaries = settings.getDisabledDictionariesPaths();
			for(String folder : settings.getDictionaryFoldersPaths())
			{
				SPFileUtil.processFilesRecursively(folder, new Consumer<String>()
				{
					@Override
					public void accept(final String s)
					{
						if(!disabledDictionaries.contains(s))
						{
							loaders.add(new FileLoader(s, s));
						}
					}
				});

			}
		}
		loaders.add(stateLoader);
		for(Loader loader : loaders)
		{
			spellChecker.loadDictionary(loader);
		}
		userDictionary = stateLoader.getDictionary();

	}


	public boolean hasProblem(@Nonnull String word)
	{
		return !spellChecker.isCorrect(word);
	}

	public void acceptWordAsCorrect(@Nonnull String word, Project project)
	{
		final String transformed = spellChecker.getTransformation().transform(word);
		if(transformed != null)
		{
			userDictionary.addToDictionary(transformed);
			final PsiModificationTracker modificationTracker = PsiManager.getInstance(project).getModificationTracker();
			modificationTracker.incCounter();
		}
	}

	public void updateUserDictionary(@Nullable Collection<String> words)
	{
		userDictionary.replaceAll(words);
		restartInspections();
	}


	@Nonnull
	public static List<String> getBundledDictionaries()
	{
		final ArrayList<String> dictionaries = new ArrayList<String>();
		for(BundledDictionaryProvider provider : BundledDictionaryProvider.EP_NAME.getExtensionList())
		{
			ContainerUtil.addAll(dictionaries, provider.getBundledDictionaries());
		}
		return dictionaries;
	}

	@Nonnull
	public static HighlightDisplayLevel getHighlightDisplayLevel()
	{
		return HighlightDisplayLevel.find(SpellcheckerSeverities.TYPO);
	}

	@Nonnull
	public List<String> getSuggestions(@Nonnull String text)
	{
		return suggestionProvider.getSuggestions(text);
	}

	@Nonnull
	protected List<String> getRawSuggestions(@Nonnull String word)
	{
		if(!spellChecker.isCorrect(word))
		{
			List<String> suggestions = spellChecker.getSuggestions(word, MAX_SUGGESTIONS_THRESHOLD, MAX_METRICS);
			if(!suggestions.isEmpty())
			{
				boolean capitalized = Strings.isCapitalized(word);
				boolean upperCases = Strings.isUpperCase(word);
				if(capitalized)
				{
					Strings.capitalize(suggestions);
				}
				else if(upperCases)
				{
					Strings.upperCase(suggestions);
				}
			}
			return new ArrayList<String>(new LinkedHashSet<String>(suggestions));
		}
		return Collections.emptyList();
	}


	public static void restartInspections()
	{
		ApplicationManager.getApplication().invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				Project[] projects = ProjectManager.getInstance().getOpenProjects();
				for(Project project : projects)
				{
					if(project.isInitialized() && project.isOpen() && !project.isDefault())
					{
						DaemonCodeAnalyzer.getInstance(project).restart();
					}
				}
			}
		});
	}


}
