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
package com.intellij.spellchecker.inspections;

import com.intellij.spellchecker.SpellCheckerManager;
import com.intellij.spellchecker.quickfixes.SpellCheckerQuickFix;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.SuppressibleSpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.TokenConsumer;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import com.intellij.spellchecker.util.SpellCheckerBundle;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.editor.inspection.*;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.refactoring.NamesValidator;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@ExtensionImpl
public class SpellCheckingInspection extends LocalInspectionTool
{
	public static final String SPELL_CHECKING_INSPECTION_TOOL_NAME = "SpellCheckingInspection";

	@Override
	@Nls
	@Nonnull
	public String getGroupDisplayName()
	{
		return SpellCheckerBundle.message("spelling");
	}

	@Override
	@Nls
	@Nonnull
	public String getDisplayName()
	{
		return SpellCheckerBundle.message("spellchecking.inspection.name");
	}

	@Nonnull
	@Override
	public SuppressQuickFix[] getBatchSuppressActions(@Nullable PsiElement element)
	{
		if(element != null)
		{
			final Language language = element.getLanguage();
			SpellcheckingStrategy strategy = getSpellcheckingStrategy(element, language);
			if(strategy instanceof SuppressibleSpellcheckingStrategy)
			{
				return ((SuppressibleSpellcheckingStrategy) strategy).getSuppressActions(element, getShortName());
			}
		}
		return super.getBatchSuppressActions(element);
	}

	private static SpellcheckingStrategy getSpellcheckingStrategy(@Nonnull PsiElement element, @Nonnull Language language)
	{
		for(SpellcheckingStrategy strategy : SpellcheckingStrategy.forLanguage(language))
		{
			if(strategy.isMyContext(element))
			{
				return strategy;
			}
		}
		return null;
	}

	@Override
	public boolean isSuppressedFor(@Nonnull PsiElement element)
	{
		final Language language = element.getLanguage();
		SpellcheckingStrategy strategy = getSpellcheckingStrategy(element, language);
		if(strategy instanceof SuppressibleSpellcheckingStrategy)
		{
			return ((SuppressibleSpellcheckingStrategy) strategy).isSuppressedFor(element, getShortName());
		}
		return super.isSuppressedFor(element);
	}

	@Override
	@Nonnull
	public String getShortName()
	{
		return SPELL_CHECKING_INSPECTION_TOOL_NAME;
	}

	@Override
	public boolean isEnabledByDefault()
	{
		return true;
	}

	@Override
	@Nonnull
	public HighlightDisplayLevel getDefaultLevel()
	{
		return SpellCheckerManager.getHighlightDisplayLevel();
	}

	@Override
	@Nonnull
	public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, final boolean isOnTheFly, LocalInspectionToolSession session, Object state)
	{
		SpellCheckingInspectionState localState = (SpellCheckingInspectionState) state;

		final SpellCheckerManager manager = SpellCheckerManager.getInstance(holder.getProject());

		return new PsiElementVisitor()
		{
			@Override
			public void visitElement(final PsiElement element)
			{

				final ASTNode node = element.getNode();
				if(node == null)
				{
					return;
				}
				// Extract parser definition from element
				final Language language = element.getLanguage();
				final IElementType elementType = node.getElementType();
				final ParserDefinition parserDefinition = ParserDefinition.forLanguage(language);

				// Handle selected options
				if(parserDefinition != null)
				{
					if(parserDefinition.getStringLiteralElements(element.getLanguageVersion()).contains(elementType))
					{
						if(!localState.processLiterals)
						{
							return;
						}
					}
					else if(parserDefinition.getCommentTokens(element.getLanguageVersion()).contains(elementType))
					{
						if(!localState.processComments)
						{
							return;
						}
					}
					else if(!localState.processCode)
					{
						return;
					}
				}

				tokenize(element, language, new MyTokenConsumer(manager, holder, NamesValidator.forLanguage(language)));
			}
		};
	}

	/**
	 * Splits element text in tokens according to spell checker strategy of given language
	 *
	 * @param element  Psi element
	 * @param language Usually element.getLanguage()
	 * @param consumer the consumer of tokens
	 */
	public static void tokenize(@Nonnull final PsiElement element, @Nonnull final Language language, TokenConsumer consumer)
	{
		final SpellcheckingStrategy factoryByLanguage = getSpellcheckingStrategy(element, language);
		if(factoryByLanguage == null)
		{
			return;
		}
		Tokenizer tokenizer = factoryByLanguage.getTokenizer(element);
		//noinspection unchecked
		tokenizer.tokenize(element, consumer);
	}


	private static void addBatchDescriptor(PsiElement element,
										   @Nonnull TextRange textRange,
										   @Nonnull ProblemsHolder holder)
	{
		SpellCheckerQuickFix[] fixes = SpellcheckingStrategy.getDefaultBatchFixes();
		ProblemDescriptor problemDescriptor = createProblemDescriptor(element, textRange, fixes, false);
		holder.registerProblem(problemDescriptor);
	}

	private static void addRegularDescriptor(PsiElement element, @Nonnull TextRange textRange, @Nonnull ProblemsHolder holder,
											 boolean useRename, String wordWithTypo)
	{
		SpellcheckingStrategy strategy = getSpellcheckingStrategy(element, element.getLanguage());

		LocalQuickFix[] fixes = strategy != null
				? strategy.getRegularFixes(element, textRange, useRename, wordWithTypo)
				: SpellcheckingStrategy.getDefaultRegularFixes(useRename, wordWithTypo, element, textRange);

		final ProblemDescriptor problemDescriptor = createProblemDescriptor(element, textRange, fixes, true);
		holder.registerProblem(problemDescriptor);
	}

	private static ProblemDescriptor createProblemDescriptor(PsiElement element, TextRange textRange,
															 LocalQuickFix[] fixes,
															 boolean onTheFly)
	{
		final String description = SpellCheckerBundle.message("typo.in.word.ref");
		return new ProblemDescriptorBase(element, element, description, fixes, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, false, textRange, onTheFly, onTheFly);
	}

	private static class MyTokenConsumer extends TokenConsumer implements Consumer<TextRange>
	{
		private final Set<String> myAlreadyChecked = new HashSet<>();
		private final SpellCheckerManager myManager;
		private final ProblemsHolder myHolder;
		private final NamesValidator myNamesValidator;
		private PsiElement myElement;
		private String myText;
		private boolean myUseRename;
		private int myOffset;

		public MyTokenConsumer(SpellCheckerManager manager, ProblemsHolder holder, NamesValidator namesValidator)
		{
			myManager = manager;
			myHolder = holder;
			myNamesValidator = namesValidator;
		}

		@Override
		public void consumeToken(final PsiElement element, final String text, final boolean useRename, final int offset, TextRange rangeToCheck,
								 Splitter splitter)
		{
			myElement = element;
			myText = text;
			myUseRename = useRename;
			myOffset = offset;
			splitter.split(text, rangeToCheck, this);
		}

		@Override
		@RequiredReadAction
		public void accept(TextRange range)
		{
			String word = range.substring(myText);
			if(!myHolder.isOnTheFly() && myAlreadyChecked.contains(word))
			{
				return;
			}

			boolean keyword = myNamesValidator.isKeyword(word, myElement.getProject());
			if(keyword)
			{
				return;
			}

			if(myManager.hasProblem(word))
			{
				//Use tokenizer to generate accurate range in element (e.g. in case of escape sequences in element)
				SpellcheckingStrategy strategy = getSpellcheckingStrategy(myElement, myElement.getLanguage());

				final Tokenizer tokenizer = strategy != null ? strategy.getTokenizer(myElement) : null;
				if(tokenizer != null)
				{
					range = tokenizer.getHighlightingRange(myElement, myOffset, range);
				}
				assert range.getStartOffset() >= 0;

				if(myHolder.isOnTheFly())
				{
					addRegularDescriptor(myElement, range, myHolder, myUseRename, word);
				}
				else
				{
					myAlreadyChecked.add(word);
					addBatchDescriptor(myElement, range, myHolder);
				}
			}
		}
	}
}
