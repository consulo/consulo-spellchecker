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

import com.intellij.spellchecker.util.SpellCheckerBundle;
import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

@ExtensionImpl
public class SpellCheckerConfigurable implements SearchableConfigurable, Configurable.NoScroll, ProjectConfigurable
{
	private SpellCheckerSettingsPane myPanel;
	private final SpellCheckerSettings mySpellCheckerSettings;
	private final Project myProject;

	@Inject
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

	@Nullable
	@Override
	public String getParentId()
	{
		return StandardConfigurableIds.EDITOR_GROUP;
	}

	@Override
	@Nonnull
	public String getId()
	{
		return "reference.settings.ide.settings.spelling";
	}

	@Override
	public Runnable enableSearch(String option)
	{
		return null;
	}

	@RequiredUIAccess
	@Override
	public JComponent createComponent(@Nonnull Disposable uiDisposable)
	{
		if(myPanel == null)
		{
			myPanel = new SpellCheckerSettingsPane(mySpellCheckerSettings, myProject);
		}
		return myPanel;
	}

	@RequiredUIAccess
	@Override
	public boolean isModified()
	{
		return myPanel == null || myPanel.isModified();
	}

	@RequiredUIAccess
	@Override
	public void apply() throws ConfigurationException
	{
		if(myPanel != null)
		{
			myPanel.apply();
		}
	}

	@RequiredUIAccess
	@Override
	public void reset()
	{
		if(myPanel != null)
		{
			myPanel.reset();
		}
	}

	@RequiredUIAccess
	@Override
	public void disposeUIResources()
	{
		if(myPanel!= null)
		{
			Disposer.dispose(myPanel);
			myPanel = null;
		}
	}
}
