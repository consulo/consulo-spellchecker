/*
 * Copyright 2013 Consulo.org
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
package com.intellij.spellchecker.vcs;

import com.intellij.spellchecker.ui.SpellCheckingEditorCustomization;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.language.editor.ui.EditorCustomization;
import consulo.language.editor.ui.SpellCheckerCustomization;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12:12/03.07.13
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class SpellCheckerCustomizationImpl extends SpellCheckerCustomization
{
	public SpellCheckerCustomizationImpl()
	{
		setInstance(this);
	}

	@Nonnull
	@Override
	public EditorCustomization getCustomization(boolean enabled)
	{
		return SpellCheckingEditorCustomization.getInstance(enabled);
	}

	@Override
	public boolean isEnabled()
	{
		return true;
	}
}
