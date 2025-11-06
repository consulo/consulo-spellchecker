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

import com.intellij.spellchecker.SimpleSpellcheckerEngine;
import com.intellij.spellchecker.SpellCheckerManager;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.editor.inspection.*;
import consulo.language.editor.refactoring.NamesValidator;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.spellchecker.editor.SpellcheckerEngineManager;
import consulo.language.spellchecker.editor.inspection.SpellcheckerInspection;
import consulo.language.spellcheker.SpellcheckingStrategy;
import consulo.language.spellcheker.tokenizer.TokenConsumer;
import consulo.language.spellcheker.tokenizer.Tokenizer;
import consulo.language.spellcheker.tokenizer.splitter.TokenSplitter;
import consulo.localize.LocalizeValue;
import consulo.spellchecker.localize.SpellCheckerLocalize;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@ExtensionImpl
public class SpellCheckingInspection extends SpellcheckerInspection {
    public static final String SPELL_CHECKING_INSPECTION_TOOL_NAME = "SpellCheckingInspection";

    @Inject
    protected SpellCheckingInspection(SpellcheckerEngineManager spellcheckerEngineManager) {
        super(spellcheckerEngineManager, SimpleSpellcheckerEngine.ID);
    }

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return SpellCheckerLocalize.spelling();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return SpellCheckerLocalize.spellcheckingInspectionName();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return SPELL_CHECKING_INSPECTION_TOOL_NAME;
    }

    @Nonnull
    @Override
    public InspectionToolState<?> createStateProvider() {
        return new SpellCheckingInspectionState();
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitorImpl(
        @Nonnull final ProblemsHolder holder,
        boolean isOnTheFly,
        @Nonnull LocalInspectionToolSession session,
        @Nonnull Object state
    ) {
        SpellCheckingInspectionState localState = (SpellCheckingInspectionState) state;

        final SpellCheckerManager manager = SpellCheckerManager.getInstance(holder.getProject());

        return new PsiElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitElement(PsiElement element) {
                ASTNode node = element.getNode();
                if (node == null) {
                    return;
                }
                // Extract parser definition from element
                Language language = element.getLanguage();
                IElementType elementType = node.getElementType();
                ParserDefinition parserDefinition = ParserDefinition.forLanguage(language);

                // Handle selected options
                if (parserDefinition != null) {
                    if (parserDefinition.getStringLiteralElements(element.getLanguageVersion()).contains(elementType)) {
                        if (!localState.processLiterals) {
                            return;
                        }
                    }
                    else if (parserDefinition.getCommentTokens(element.getLanguageVersion()).contains(elementType)) {
                        if (!localState.processComments) {
                            return;
                        }
                    }
                    else if (!localState.processCode) {
                        return;
                    }
                }

                tokenize(element, new MyTokenConsumer(manager, holder, NamesValidator.forLanguage(language)));
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
    @RequiredReadAction
    public static void tokenize(@Nonnull PsiElement element, TokenConsumer consumer) {
        SpellcheckingStrategy factoryByLanguage = getSpellcheckingStrategy(element);
        if (factoryByLanguage == null) {
            return;
        }
        Tokenizer tokenizer = factoryByLanguage.getTokenizer(element);
        //noinspection unchecked
        tokenizer.tokenize(element, consumer);
    }

    @RequiredReadAction
    private static void addBatchDescriptor(PsiElement element, @Nonnull TextRange textRange, @Nonnull ProblemsHolder holder) {
        holder.newProblem(SpellCheckerLocalize.typoInWordRef())
            .range(element, textRange)
            .withFixes(SpellcheckerQuickFixes.getDefaultBatchFixes())
            .create();
    }

    @RequiredReadAction
    private static void addRegularDescriptor(
        PsiElement element,
        @Nonnull TextRange textRange,
        @Nonnull ProblemsHolder holder,
        boolean useRename,
        String wordWithTypo
    ) {
        SpellcheckingStrategy strategy = getSpellcheckingStrategy(element);

        LocalQuickFix[] fixes = strategy != null
            ? SpellcheckerQuickFixes.getRegularFixes(element, textRange, useRename, wordWithTypo)
            : SpellcheckerQuickFixes.getDefaultRegularFixes(useRename, wordWithTypo, element, textRange);

        holder.newProblem(SpellCheckerLocalize.typoInWordRef())
            .range(element, textRange)
            .withFixes(fixes)
            .onTheFly()
            .create();
    }

    private static class MyTokenConsumer extends TokenConsumer implements Consumer<TextRange> {
        private final Set<String> myAlreadyChecked = new HashSet<>();
        private final SpellCheckerManager myManager;
        private final ProblemsHolder myHolder;
        private final NamesValidator myNamesValidator;
        private PsiElement myElement;
        private String myText;
        private boolean myUseRename;
        private int myOffset;

        public MyTokenConsumer(SpellCheckerManager manager, ProblemsHolder holder, NamesValidator namesValidator) {
            myManager = manager;
            myHolder = holder;
            myNamesValidator = namesValidator;
        }

        @Override
        @RequiredReadAction
        public void consumeToken(
            PsiElement element,
            String text,
            boolean useRename,
            int offset,
            TextRange rangeToCheck,
            TokenSplitter splitter
        ) {
            myElement = element;
            myText = text;
            myUseRename = useRename;
            myOffset = offset;
            splitter.split(text, rangeToCheck, this);
        }

        @Override
        @RequiredReadAction
        public void accept(TextRange range) {
            String word = range.substring(myText);
            if (!myHolder.isOnTheFly() && myAlreadyChecked.contains(word)) {
                return;
            }

            boolean keyword = myNamesValidator.isKeyword(word, myElement.getProject());
            if (keyword) {
                return;
            }

            if (myManager.hasProblem(word)) {
                //Use tokenizer to generate accurate range in element (e.g. in case of escape sequences in element)
                SpellcheckingStrategy strategy = getSpellcheckingStrategy(myElement);

                Tokenizer tokenizer = strategy != null ? strategy.getTokenizer(myElement) : null;
                if (tokenizer != null) {
                    range = tokenizer.getHighlightingRange(myElement, myOffset, range);
                }
                assert range.getStartOffset() >= 0;

                if (myHolder.isOnTheFly()) {
                    addRegularDescriptor(myElement, range, myHolder, myUseRename, word);
                }
                else {
                    myAlreadyChecked.add(word);
                    myHolder.newProblem(SpellCheckerLocalize.typoInWordRef())
                        .range(myElement, range)
                        .withFixes(SpellcheckerQuickFixes.getDefaultBatchFixes())
                        .create();
                }
            }
        }
    }
}
