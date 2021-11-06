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
package com.intellij.spellchecker.tokenizer;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.SuppressionUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.spellchecker.inspections.PlainTextSplitter;
import com.intellij.spellchecker.quickfixes.*;
import consulo.annotation.access.RequiredReadAction;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class SpellcheckingStrategy
{
	protected final Tokenizer<PsiComment> myCommentTokenizer = new CommentTokenizer();
	protected final Tokenizer<PsiNameIdentifierOwner> myNameIdentifierOwnerTokenizer = new PsiIdentifierOwnerTokenizer();

	public static final Tokenizer EMPTY_TOKENIZER = new Tokenizer()
	{
		@Override
		public void tokenize(@Nonnull PsiElement element, TokenConsumer consumer)
		{
		}
	};

	public static final Tokenizer<PsiElement> TEXT_TOKENIZER = new TokenizerBase<>(PlainTextSplitter.getInstance());

	private static final SpellCheckerQuickFix[] BATCH_FIXES = new SpellCheckerQuickFix[]{new AcceptWordAsCorrect()};

	@Nonnull
	@RequiredReadAction
	public Tokenizer getTokenizer(PsiElement element)
	{
		if(element instanceof PsiWhiteSpace)
		{
			return EMPTY_TOKENIZER;
		}

		if(element instanceof PsiLanguageInjectionHost && InjectedLanguageUtil.hasInjections((PsiLanguageInjectionHost) element))
		{
			return EMPTY_TOKENIZER;
		}

		if(element instanceof PsiNameIdentifierOwner)
		{
			return myNameIdentifierOwnerTokenizer;
		}

		if(element instanceof PsiComment)
		{
			if(SuppressionUtil.isSuppressionComment(element))
			{
				return EMPTY_TOKENIZER;
			}

			//don't check shebang
			if(element.getTextOffset() == 0 && element.getText().startsWith("#!"))
			{
				return EMPTY_TOKENIZER;
			}
			return myCommentTokenizer;
		}

		if(element instanceof PsiPlainText)
		{
			return TEXT_TOKENIZER;
		}
		return EMPTY_TOKENIZER;
	}

	public LocalQuickFix[] getRegularFixes(PsiElement element,
										   @Nonnull TextRange textRange,
										   boolean useRename,
										   String typo)
	{
		return getDefaultRegularFixes(useRename, typo, element, textRange);
	}

	public static LocalQuickFix[] getDefaultRegularFixes(boolean useRename, String typo, @Nullable PsiElement element, @Nonnull TextRange range)
	{
		ArrayList<LocalQuickFix> result = new ArrayList<>();

		if(useRename)
		{
			result.add(new RenameTo(typo));
		}
		else if(element != null)
		{
			result.add(new ChangeTo(typo, element, range));
		}

		if(element == null)
		{
			result.add(new SaveTo(typo));
			return result.toArray(LocalQuickFix.EMPTY_ARRAY);
		}

		//		final SpellCheckerSettings settings = SpellCheckerSettings.getInstance(element.getProject());
		//		if(settings.isUseSingleDictionaryToSave())
		//		{
		//			result.add(new SaveTo(typo, DictionaryLevel.getLevelByName(settings.getDictionaryToSave())));
		//			return result.toArray(LocalQuickFix.EMPTY_ARRAY);
		//		}

		result.add(new SaveTo(typo));
		return result.toArray(LocalQuickFix.EMPTY_ARRAY);
	}

	public static SpellCheckerQuickFix[] getDefaultBatchFixes()
	{
		return BATCH_FIXES;
	}

	public boolean isMyContext(@Nonnull PsiElement element)
	{
		return true;
	}
}
