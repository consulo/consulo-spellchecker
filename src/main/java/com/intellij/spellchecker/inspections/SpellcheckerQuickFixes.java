package com.intellij.spellchecker.inspections;

import com.intellij.spellchecker.quickfixes.*;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2023-03-26
 */
public class SpellcheckerQuickFixes {
    private static final SpellCheckerQuickFix[] BATCH_FIXES = new SpellCheckerQuickFix[]{new AcceptWordAsCorrect()};

    public static LocalQuickFix[] getRegularFixes(
        PsiElement element,
        @Nonnull TextRange textRange,
        boolean useRename,
        String typo
    ) {
        return getDefaultRegularFixes(useRename, typo, element, textRange);
    }

    public static LocalQuickFix[] getDefaultRegularFixes(
        boolean useRename,
        String typo,
        @Nullable PsiElement element,
        @Nonnull TextRange range
    ) {
        List<LocalQuickFix> result = new ArrayList<>();

        if (useRename) {
            result.add(new RenameTo(typo));
        }
        else if (element != null) {
            result.add(new ChangeTo(typo, element, range));
        }

        if (element == null) {
            result.add(new SaveTo(typo));
            return result.toArray(LocalQuickFix.EMPTY_ARRAY);
        }

        /*
        SpellCheckerSettings settings = SpellCheckerSettings.getInstance(element.getProject());
        if (settings.isUseSingleDictionaryToSave()) {
            result.add(new SaveTo(typo, DictionaryLevel.getLevelByName(settings.getDictionaryToSave())));
            return result.toArray(LocalQuickFix.EMPTY_ARRAY);
        }
        */

        result.add(new SaveTo(typo));
        return result.toArray(LocalQuickFix.EMPTY_ARRAY);
    }

    public static SpellCheckerQuickFix[] getDefaultBatchFixes() {
        return BATCH_FIXES;
    }
}
