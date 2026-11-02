package com.intellij.spellchecker;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.spellchecker.editor.SpellcheckerEngine;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.spellchecker.localize.SpellCheckerLocalize;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * English spellchecker engine backed by bundled word lists ({@link SimpleDictionaryProvider}).
 *
 * @author VISTALL
 * @since 2023-03-26
 */
@ExtensionImpl
public class SimpleSpellcheckerEngine implements SpellcheckerEngine {
    public static final String ID = "simple-spellchecker";

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return SpellCheckerLocalize.simpleSpellcheckerEngineName();
    }

    @Override
    public Set<Locale> getLocales() {
        return Set.of(Locale.ENGLISH);
    }

    @Override
    public boolean hasProblem(@Nonnull Project project, @Nonnull String word) {
        return SimpleSpellcheckerManager.getInstance(project).hasProblem(word);
    }

    @Nonnull
    @Override
    public List<String> getSuggestions(@Nonnull Project project, @Nonnull String word) {
        return SimpleSpellcheckerManager.getInstance(project).getSuggestions(word);
    }
}
