package com.intellij.spellchecker;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.spellchecker.editor.SpellcheckerEngine;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
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
        return LocalizeValue.localizeTODO("Simple Spellchecker");
    }

    @Nonnull
    @Override
    public List<String> getSuggestions(@Nonnull Project project, @Nonnull String text) {
        return SpellCheckerManager.getInstance(project).getSuggestions(text);
    }

    @Override
    public boolean hasProblem(@Nonnull Project project, @Nonnull String text) {
        return SpellCheckerManager.getInstance(project).hasProblem(text);
    }

    @Override
    public boolean canSaveUserWords(@Nonnull Project project) {
        return true;
    }

    @Override
    public void acceptWordAsCorrect(@Nonnull Project project, @Nonnull String word) {
        SpellCheckerManager.getInstance(project).acceptWordAsCorrect(word, project);
    }
}
