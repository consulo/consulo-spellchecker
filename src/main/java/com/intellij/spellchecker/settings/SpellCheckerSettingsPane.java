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
package com.intellij.spellchecker.settings;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.newEditor.OptionsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.profile.codeInspection.ui.ErrorsConfigurable;
import com.intellij.spellchecker.SpellCheckerManager;
import com.intellij.spellchecker.dictionary.EditableDictionary;
import com.intellij.spellchecker.inspections.SpellCheckingInspection;
import com.intellij.spellchecker.util.SPFileUtil;
import com.intellij.spellchecker.util.SpellCheckerBundle;
import com.intellij.spellchecker.util.Strings;
import com.intellij.ui.AddDeleteListPanel;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.OptionalChooserComponent;
import com.intellij.ui.PathsChooserComponent;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Consumer;

public class SpellCheckerSettingsPane extends JPanel implements Disposable
{
	private OptionalChooserComponent<String> myOptionalChooserComponent;
	private PathsChooserComponent myPathsChooserComponent;
	private final List<Pair<String, Boolean>> myAllDictionaries = new ArrayList<Pair<String, Boolean>>();
	private final List<String> myDictionariesFolders = new ArrayList<String>();
	private final List<String> myRemovedDictionaries = new ArrayList<String>();
	private final WordsPanel myWordsPanel;
	private final SpellCheckerManager myManager;
	private final SpellCheckerSettings mySpellCheckerSettings;

	public SpellCheckerSettingsPane(SpellCheckerSettings settings, final Project project)
	{
		super(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, true));

		mySpellCheckerSettings = settings;
		myManager = SpellCheckerManager.getInstance(project);
		HyperlinkLabel link = new HyperlinkLabel(SpellCheckerBundle.message("link.to.inspection.settings"));
		link.addHyperlinkListener(new HyperlinkListener()
		{
			@Override
			public void hyperlinkUpdate(final HyperlinkEvent e)
			{
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
				{
					final OptionsEditor optionsEditor = DataManager.getInstance().getDataContext().getData(OptionsEditor.KEY);
					if(optionsEditor != null)
					{
						final ErrorsConfigurable errorsConfigurable = optionsEditor.findConfigurable(ErrorsConfigurable.class);
						if(errorsConfigurable != null)
						{
							optionsEditor.clearSearchAndSelect(errorsConfigurable).doWhenDone(new Runnable()
							{
								@Override
								public void run()
								{
									errorsConfigurable.selectInspectionTool(SpellCheckingInspection.SPELL_CHECKING_INSPECTION_TOOL_NAME);
								}
							});
						}
					}
				}
			}
		});

		JPanel linkContainer = new JPanel(new BorderLayout());
		linkContainer.setPreferredSize(new Dimension(24, 38));
		linkContainer.add(link, BorderLayout.CENTER);
		add(linkContainer);

		// Fill in all the dictionaries folders (not implemented yet) and enabled dictionaries
		fillAllDictionaries();


		myPathsChooserComponent = new PathsChooserComponent(myDictionariesFolders, new PathsChooserComponent.PathProcessor()
		{
			@Override
			public boolean addPath(List<String> paths, String path)
			{
				if(paths.contains(path))
				{
					final String title = SpellCheckerBundle.message("add.directory.title");
					final String msg = SpellCheckerBundle.message("directory.is.already.included");
					Messages.showErrorDialog(SpellCheckerSettingsPane.this, msg, title);
					return false;
				}
				paths.add(path);

				final ArrayList<Pair<String, Boolean>> currentDictionaries = myOptionalChooserComponent.getCurrentModel();
				SPFileUtil.processFilesRecursively(path, new Consumer<String>()
				{
					@Override
					public void consume(final String s)
					{
						currentDictionaries.add(Pair.create(s, true));
					}
				});
				myOptionalChooserComponent.refresh();
				return true;
			}

			@Override
			public boolean removePath(List<String> paths, String path)
			{
				if(paths.remove(path))
				{
					final ArrayList<Pair<String, Boolean>> result = new ArrayList<Pair<String, Boolean>>();
					final ArrayList<Pair<String, Boolean>> currentDictionaries = myOptionalChooserComponent.getCurrentModel();
					for(Pair<String, Boolean> pair : currentDictionaries)
					{
						if(!pair.first.startsWith(FileUtil.toSystemDependentName(path)))
						{
							result.add(pair);
						}
						else
						{
							myRemovedDictionaries.add(pair.first);
						}
					}
					currentDictionaries.clear();
					currentDictionaries.addAll(result);
					myOptionalChooserComponent.refresh();
					return true;
				}
				return false;
			}
		}, project);

		myPathsChooserComponent.getEmptyText().setText(SpellCheckerBundle.message("no.custom.folders"));

		myOptionalChooserComponent = new OptionalChooserComponent<String>(myAllDictionaries)
		{
			@Override
			public JCheckBox createCheckBox(String path, boolean checked)
			{
				if(isUserDictionary(path))
				{
					path = FileUtil.toSystemIndependentName(path);
					final int i = path.lastIndexOf('/');
					if(i != -1)
					{
						final String name = path.substring(i + 1);
						return new JCheckBox("[user] " + name, checked);
					}
				}
				return new JCheckBox("[bundled] " + FileUtil.toSystemDependentName(path), checked);
			}
		};

		myWordsPanel = new WordsPanel(myManager);

		TabbedPaneWrapper tabbedPaneWrapper = new TabbedPaneWrapper(this);
		tabbedPaneWrapper.addTab("Accepted Words", myWordsPanel);

		JPanel secondPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, true));

		JPanel customDictionariesPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, true));
		customDictionariesPanel.setBorder(IdeBorderFactory.createTitledBorder(SpellCheckerBundle.message("add.directory.title"), false));
		customDictionariesPanel.add(new JBLabel(SpellCheckerBundle.message("add.directory.description")));
		customDictionariesPanel.add(myPathsChooserComponent.getContentPane());
		secondPanel.add(customDictionariesPanel);

		myOptionalChooserComponent.getEmptyText().setText(SpellCheckerBundle.message("no.dictionaries"));
		JPanel dictionariesPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, true));
		dictionariesPanel.setBorder(IdeBorderFactory.createTitledBorder(SpellCheckerBundle.message("dictionaries.panel.title"), false));
		dictionariesPanel.add(new JBLabel(SpellCheckerBundle.message("dictionaries.panel.description")));
		dictionariesPanel.add(ScrollPaneFactory.createScrollPane(myOptionalChooserComponent.getContentPane()));
		secondPanel.add(dictionariesPanel);

		tabbedPaneWrapper.addTab("Dictionaries", secondPanel);

		add(tabbedPaneWrapper.getComponent());
	}

	public boolean isModified()
	{
		return myWordsPanel.isModified() || myOptionalChooserComponent.isModified() || myPathsChooserComponent.isModified();
	}

	public void apply() throws ConfigurationException
	{
		if(myWordsPanel.isModified())
		{
			myManager.updateUserDictionary(myWordsPanel.getWords());
		}
		if(!myOptionalChooserComponent.isModified() && !myPathsChooserComponent.isModified())
		{
			return;
		}

		myOptionalChooserComponent.apply();
		myPathsChooserComponent.apply();
		mySpellCheckerSettings.setDictionaryFoldersPaths(myPathsChooserComponent.getValues());

		final HashSet<String> disabledDictionaries = new HashSet<String>();
		final HashSet<String> bundledDisabledDictionaries = new HashSet<String>();
		for(Pair<String, Boolean> pair : myAllDictionaries)
		{
			if(!pair.second)
			{
				final String scriptPath = pair.first;
				if(isUserDictionary(scriptPath))
				{
					disabledDictionaries.add(scriptPath);
				}
				else
				{
					bundledDisabledDictionaries.add(scriptPath);
				}
			}

		}
		mySpellCheckerSettings.setDisabledDictionariesPaths(disabledDictionaries);
		mySpellCheckerSettings.setBundledDisabledDictionariesPaths(bundledDisabledDictionaries);

		myManager.updateBundledDictionaries(myRemovedDictionaries);
	}

	private boolean isUserDictionary(final String dictionary)
	{
		boolean isUserDictionary = false;
		for(String dictionaryFolder : myPathsChooserComponent.getValues())
		{
			if(FileUtil.toSystemIndependentName(dictionary).startsWith(dictionaryFolder))
			{
				isUserDictionary = true;
				break;
			}
		}
		return isUserDictionary;

	}

	public void reset()
	{
		myPathsChooserComponent.reset();
		fillAllDictionaries();
		myOptionalChooserComponent.reset();
		myRemovedDictionaries.clear();
	}


	private void fillAllDictionaries()
	{
		myDictionariesFolders.clear();
		myDictionariesFolders.addAll(mySpellCheckerSettings.getDictionaryFoldersPaths());
		myAllDictionaries.clear();
		for(String dictionary : SpellCheckerManager.getBundledDictionaries())
		{
			myAllDictionaries.add(Pair.create(dictionary, !mySpellCheckerSettings.getBundledDisabledDictionariesPaths().contains(dictionary)));
		}

		// user
		//todo [shkate]: refactoring  - SpellCheckerManager contains the same code withing reloadConfiguration()
		final Set<String> disabledDictionaries = mySpellCheckerSettings.getDisabledDictionariesPaths();
		for(String folder : myDictionariesFolders)
		{
			SPFileUtil.processFilesRecursively(folder, new Consumer<String>()
			{
				@Override
				public void consume(final String s)
				{
					myAllDictionaries.add(Pair.create(s, !disabledDictionaries.contains(s)));
				}
			});
		}
	}


	@Override
	public void dispose()
	{
		if(myWordsPanel != null)
		{
			myWordsPanel.dispose();
		}
	}

	public static final class WordDescriber
	{
		private final EditableDictionary dictionary;

		public WordDescriber(EditableDictionary dictionary)
		{
			this.dictionary = dictionary;
		}

		@NotNull
		public List<String> process()
		{
			if(this.dictionary == null)
			{
				return new ArrayList<String>();
			}
			Set<String> words = this.dictionary.getEditableWords();
			if(words == null)
			{
				return new ArrayList<String>();
			}
			List<String> result = new ArrayList<String>();
			for(String word : words)
			{
				result.add(word);
			}
			Collections.sort(result);
			return result;
		}
	}

	private static final class WordsPanel extends AddDeleteListPanel<String> implements Disposable
	{
		private final SpellCheckerManager manager;

		private WordsPanel(SpellCheckerManager manager)
		{
			super(null, new WordDescriber(manager.getUserDictionary()).process());
			this.manager = manager;
			getEmptyText().setText(SpellCheckerBundle.message("no.words"));
		}


		@Override
		protected String findItemToAdd()
		{
			String word = Messages.showInputDialog(SpellCheckerBundle.message("enter.simple.word"), SpellCheckerBundle.message("add.new.word"),
					null);
			if(word == null)
			{
				return null;
			}
			else
			{
				word = word.trim();
			}

			if(Strings.isMixedCase(word))
			{
				Messages.showWarningDialog(SpellCheckerBundle.message("entered.word.0.is.mixed.cased.you.must.enter.simple.word", word),
						SpellCheckerBundle.message("add.new.word"));
				return null;
			}
			if(!manager.hasProblem(word))
			{
				Messages.showWarningDialog(SpellCheckerBundle.message("entered.word.0.is.correct.you.no.need.to.add.this.in.list", word),
						SpellCheckerBundle.message("add.new.word"));
				return null;
			}
			return word;
		}


		@Override
		public void dispose()
		{
			myListModel.removeAllElements();
		}

		@Nullable
		public List<String> getWords()
		{
			Object[] pairs = getListItems();
			if(pairs == null)
			{
				return null;
			}
			List<String> words = new ArrayList<String>();
			for(Object pair : pairs)
			{
				words.add(pair.toString());
			}
			return words;
		}

		public boolean isModified()
		{
			List<String> newWords = getWords();
			Set<String> words = manager.getUserDictionary().getEditableWords();
			if(words == null && newWords == null)
			{
				return false;
			}
			if(words == null || newWords == null || newWords.size() != words.size())
			{
				return true;
			}
			return !(words.containsAll(newWords) && newWords.containsAll(words));
		}
	}


}
