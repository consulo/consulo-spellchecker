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

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ui.SingleCheckboxOptionsPanel;
import com.intellij.lang.*;
import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.IElementType;
import com.intellij.spellchecker.SpellCheckerManager;
import com.intellij.spellchecker.quickfixes.SpellCheckerQuickFix;
import com.intellij.spellchecker.tokenizer.*;
import com.intellij.spellchecker.util.SpellCheckerBundle;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

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
		for(SpellcheckingStrategy strategy : LanguageSpellchecking.INSTANCE.allForLanguage(language))
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
	@NonNls
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
	public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, final boolean isOnTheFly)
	{
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
				final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language);

				// Handle selected options
				if(parserDefinition != null)
				{
					if(parserDefinition.getStringLiteralElements(element.getLanguageVersion()).contains(elementType))
					{
						if(!processLiterals)
						{
							return;
						}
					}
					else if(parserDefinition.getCommentTokens(element.getLanguageVersion()).contains(elementType))
					{
						if(!processComments)
						{
							return;
						}
					}
					else if(!processCode)
					{
						return;
					}
				}

				tokenize(element, language, new MyTokenConsumer(manager, holder, LanguageNamesValidation.INSTANCE.forLanguage(language)));
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


	private static void addBatchDescriptor(PsiElement element, int offset, @Nonnull TextRange textRange, @Nonnull ProblemsHolder holder)
	{
		final SpellcheckingStrategy strategy = getSpellcheckingStrategy(element, element.getLanguage());

		SpellCheckerQuickFix[] fixes = strategy != null ? strategy.getBatchFixes(element, offset,
				textRange) : SpellcheckingStrategy.getDefaultBatchFixes();
		final ProblemDescriptor problemDescriptor = createProblemDescriptor(element, offset, textRange, holder, fixes, false);
		holder.registerProblem(problemDescriptor);
	}

	private static void addRegularDescriptor(PsiElement element, int offset, @Nonnull TextRange textRange, @Nonnull ProblemsHolder holder,
			boolean useRename, String wordWithTypo)
	{
		SpellcheckingStrategy strategy = getSpellcheckingStrategy(element, element.getLanguage());

		SpellCheckerQuickFix[] fixes = strategy != null ? strategy.getRegularFixes(element, offset, textRange, useRename,
				wordWithTypo) : SpellcheckingStrategy.getDefaultRegularFixes(useRename, wordWithTypo);

		final ProblemDescriptor problemDescriptor = createProblemDescriptor(element, offset, textRange, holder, fixes, true);
		holder.registerProblem(problemDescriptor);
	}

	private static ProblemDescriptor createProblemDescriptor(PsiElement element, int offset, TextRange textRange, ProblemsHolder holder,
			SpellCheckerQuickFix[] fixes, boolean onTheFly)
	{
		final String description = SpellCheckerBundle.message("typo.in.word.ref");
		final TextRange highlightRange = TextRange.from(offset + textRange.getStartOffset(), textRange.getLength());
		assert highlightRange.getStartOffset() >= 0;

		return holder.getManager().createProblemDescriptor(element, highlightRange, description, ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
				onTheFly, fixes);
	}

	@SuppressWarnings({"PublicField"})
	public boolean processCode = true;
	public boolean processLiterals = true;
	public boolean processComments = true;

	@Override
	public JComponent createOptionsPanel()
	{
		final Box verticalBox = Box.createVerticalBox();
		verticalBox.add(new SingleCheckboxOptionsPanel(SpellCheckerBundle.message("process.code"), this, "processCode"));
		verticalBox.add(new SingleCheckboxOptionsPanel(SpellCheckerBundle.message("process.literals"), this, "processLiterals"));
		verticalBox.add(new SingleCheckboxOptionsPanel(SpellCheckerBundle.message("process.comments"), this, "processComments"));
	/*HyperlinkLabel linkToSettings = new HyperlinkLabel(SpellCheckerBundle.message("link.to.settings"));
    linkToSettings.addHyperlinkListener(new HyperlinkListener() {
      public void hyperlinkUpdate(final HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          final OptionsEditor optionsEditor = OptionsEditor.KEY.getData(DataManager.getInstance().getDataContext());
          // ??project?

        }
      }
    });

    verticalBox.add(linkToSettings);*/
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(verticalBox, BorderLayout.NORTH);
		return panel;

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
		public void consume(TextRange textRange)
		{
			final String word = textRange.substring(myText);
			if(myHolder.isOnTheFly() && myAlreadyChecked.contains(word))
			{
				return;
			}

			boolean keyword = myNamesValidator.isKeyword(word, myElement.getProject());
			if(keyword)
			{
				return;
			}

			boolean hasProblems = myManager.hasProblem(word);
			if(hasProblems)
			{
				if(myHolder.isOnTheFly())
				{
					addRegularDescriptor(myElement, myOffset, textRange, myHolder, myUseRename, word);
				}
				else
				{
					myAlreadyChecked.add(word);
					addBatchDescriptor(myElement, myOffset, textRange, myHolder);
				}
			}
		}
	}
}
