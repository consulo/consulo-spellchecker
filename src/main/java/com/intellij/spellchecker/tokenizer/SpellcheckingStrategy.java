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

import com.intellij.codeInspection.SuppressionUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.spellchecker.inspections.PlainTextSplitter;
import com.intellij.spellchecker.quickfixes.AcceptWordAsCorrect;
import com.intellij.spellchecker.quickfixes.ChangeTo;
import com.intellij.spellchecker.quickfixes.RenameTo;
import com.intellij.spellchecker.quickfixes.SpellCheckerQuickFix;
import consulo.annotation.access.RequiredReadAction;

import javax.annotation.Nonnull;

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

	public SpellCheckerQuickFix[] getRegularFixes(PsiElement element, int offset, @Nonnull TextRange textRange, boolean useRename, String wordWithTypo)
	{
		return getDefaultRegularFixes(useRename, wordWithTypo);
	}

	public static SpellCheckerQuickFix[] getDefaultRegularFixes(boolean useRename, String wordWithTypo)
	{
		return new SpellCheckerQuickFix[]{
				(useRename ? new RenameTo(wordWithTypo) : new ChangeTo(wordWithTypo)),
				new AcceptWordAsCorrect(wordWithTypo)
		};
	}

	public SpellCheckerQuickFix[] getBatchFixes(PsiElement element, int offset, @Nonnull TextRange textRange)
	{
		return getDefaultBatchFixes();
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
