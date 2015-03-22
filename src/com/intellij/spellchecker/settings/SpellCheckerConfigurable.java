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

import javax.swing.JComponent;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.spellchecker.util.SpellCheckerBundle;

public class SpellCheckerConfigurable implements SearchableConfigurable, Configurable.NoScroll
{
	private SpellCheckerSettingsPane myPanel;
	private final SpellCheckerSettings mySpellCheckerSettings;
	private final Project myProject;

	public SpellCheckerConfigurable(Project project, SpellCheckerSettings spellCheckerSettings)
	{
		myProject = project;
		mySpellCheckerSettings = spellCheckerSettings;
	}

	@Override
	@Nls
	public String getDisplayName()
	{
		return SpellCheckerBundle.message("spelling");
	}

	@Override
	@Nullable
	@NonNls
	public String getHelpTopic()
	{
		return "reference.settings.ide.settings.spelling";
	}

	@Override
	@NotNull
	public String getId()
	{
		return getHelpTopic();
	}

	@Override
	public Runnable enableSearch(String option)
	{
		return null;
	}

	@Override
	public JComponent createComponent()
	{
		if(myPanel == null)
		{
			myPanel = new SpellCheckerSettingsPane(mySpellCheckerSettings, myProject);
		}
		return myPanel;
	}

	@Override
	public boolean isModified()
	{
		return myPanel == null || myPanel.isModified();
	}

	@Override
	public void apply() throws ConfigurationException
	{
		if(myPanel != null)
		{
			myPanel.apply();
		}
	}

	@Override
	public void reset()
	{
		if(myPanel != null)
		{
			myPanel.reset();
		}
	}

	@Override
	public void disposeUIResources()
	{
		myPanel = null;
	}
}
