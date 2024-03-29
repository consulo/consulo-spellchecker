/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@State(name = "CachedDictionaryState", storages = @Storage("cachedDictionary.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class CachedDictionaryState extends DictionaryState implements PersistentStateComponent<DictionaryState>
{
	public static final String DEFAULT_NAME = "cached";

	@Inject
	public CachedDictionaryState()
	{
		name = DEFAULT_NAME;
	}

	@Override
	public void loadState(DictionaryState state)
	{
		if(state.name == null)
		{
			state.name = DEFAULT_NAME;
		}
		super.loadState(state);
	}
}