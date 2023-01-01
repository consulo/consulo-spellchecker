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


import com.intellij.spellchecker.util.SpellCheckerBundle;
import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.util.PsiEditorUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ChangeTo extends ShowSuggestions implements LocalQuickFix, Iconable
{
	private final TextRange myRange;
	private final SmartPsiElementPointer<PsiElement> myElementPointer;

	public ChangeTo(String wordWithTypo, PsiElement element, TextRange range)
	{
		super(wordWithTypo);
		myRange = range;
		myElementPointer = SmartPointerManager.createPointer(element);
	}

	@Nonnull
	public String getName()
	{
		return SpellCheckerBundle.message("change.to");
	}

	@Nonnull
	public String getFamilyName()
	{
		return SpellCheckerBundle.message("change.to");
	}

	@Override
	@RequiredReadAction
	public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor)
	{
		PsiElement element = myElementPointer.getElement();
		if(element == null)
		{
			return;
		}
		Editor editor = PsiEditorUtil.findEditor(element);

		if(editor == null)
		{
			return;
		}

		TextRange textRange = myRange;
		editor.getSelectionModel().setSelection(textRange.getStartOffset(), textRange.getEndOffset());

		String word = editor.getSelectionModel().getSelectedText();

		if(word == null || StringUtil.isEmpty(word))
		{
			return;
		}

		List<LookupElement> lookupItems = new ArrayList<>();
		for(String variant : getSuggestions(project))
		{
			lookupItems.add(LookupElementBuilder.create(variant));
		}
		LookupElement[] items = new LookupElement[lookupItems.size()];
		items = lookupItems.toArray(items);
		LookupManager lookupManager = LookupManager.getInstance(project);
		lookupManager.showLookup(editor, items);
	}
}