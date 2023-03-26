package com.intellij.spellchecker;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.spellchecker.editor.SpellcheckerEngine;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 26/03/2023
 */
@ExtensionImpl
public class SimpleSpellcheckerEngine implements SpellcheckerEngine
{
	public static final String ID = "simple-spellchecker";

	@Nonnull
	@Override
	public String getId()
	{
		return ID;
	}

	@Nonnull
	@Override
	public String getDisplayName()
	{
		return "Simple Spellchecker";
	}
}
