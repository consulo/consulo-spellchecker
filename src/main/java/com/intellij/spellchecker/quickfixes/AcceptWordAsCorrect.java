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
package com.intellij.spellchecker.quickfixes;

import javax.annotation.Nonnull;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemDescriptorUtil;
import com.intellij.openapi.actionSystem.Anchor;
import com.intellij.openapi.project.Project;
import com.intellij.spellchecker.SpellCheckerIcons;
import com.intellij.spellchecker.SpellCheckerManager;
import com.intellij.spellchecker.util.SpellCheckerBundle;
import consulo.ui.image.Image;

public class AcceptWordAsCorrect implements SpellCheckerQuickFix
{
	private String myWord;

	public AcceptWordAsCorrect(String word)
	{
		myWord = word;
	}

	public AcceptWordAsCorrect()
	{
	}

	@Override
	@Nonnull
	public String getName()
	{
		return myWord != null ? SpellCheckerBundle.message("add.0.to.dictionary", myWord) : SpellCheckerBundle.message("add.to.dictionary");
	}

	@Override
	@Nonnull
	public String getFamilyName()
	{
		return SpellCheckerBundle.message("spelling");
	}

	@Override
	@Nonnull
	public Anchor getPopupActionAnchor()
	{
		return Anchor.LAST;
	}

	@Override
	public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor)
	{
		SpellCheckerManager spellCheckerManager = SpellCheckerManager.getInstance(project);
		if(myWord != null)
		{
			spellCheckerManager.acceptWordAsCorrect(myWord, project);
		}
		else
		{
			spellCheckerManager.acceptWordAsCorrect(ProblemDescriptorUtil.extractHighlightedText(descriptor, descriptor.getPsiElement()), project);
		}
	}

	@Override
	public Image getIcon(int flags)
	{
		return SpellCheckerIcons.Spellcheck;
	}
}
