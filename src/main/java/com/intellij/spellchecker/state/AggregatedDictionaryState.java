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

import com.intellij.spellchecker.dictionary.AggregatedDictionary;
import com.intellij.spellchecker.dictionary.ProjectDictionary;
import com.intellij.spellchecker.dictionary.UserDictionary;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.platform.Platform;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class AggregatedDictionaryState
{

	private ProjectDictionaryState projectDictionaryState;
	private CachedDictionaryState cachedDictionaryState;
	private AggregatedDictionary dictionary;
	private String currentUser;
	private Project project;

	@Inject
	public AggregatedDictionaryState()
	{
	}

	public void setProject(Project project)
	{
		this.project = project;
	}

	public void setCurrentUser(String currentUser)
	{
		this.currentUser = currentUser;
	}

	public void setDictionary(AggregatedDictionary dictionary)
	{
		this.dictionary = dictionary;
		cachedDictionaryState.setDictionary(dictionary.getCachedDictionary());
		projectDictionaryState.setProjectDictionary(dictionary.getProjectDictionary());
	}

	public AggregatedDictionary getDictionary()
	{
		return dictionary;
	}

	public void loadState()
	{
		assert project != null;
		cachedDictionaryState = Application.get().getInstance(CachedDictionaryState.class);
		projectDictionaryState = project.getInstance(ProjectDictionaryState.class);
		currentUser = Platform.current().user().name();
		retrieveDictionaries();
	}

	private void retrieveDictionaries()
	{
		ProjectDictionary projectDictionary = projectDictionaryState.getProjectDictionary();
		projectDictionary.setActiveName(currentUser);

		if(cachedDictionaryState.getDictionary() == null)
		{
			cachedDictionaryState.setDictionary(new UserDictionary(CachedDictionaryState.DEFAULT_NAME));
		}
		dictionary = new AggregatedDictionary(projectDictionary, cachedDictionaryState.getDictionary());
		setDictionary(dictionary);
	}

	@Override
	public String toString()
	{
		return "AggregatedDictionaryState{" + "dictionary=" + dictionary + '}';
	}
}
