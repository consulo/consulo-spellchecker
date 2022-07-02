package com.intellij.spellchecker.tokenizer;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.plain.PlainTextLanguage;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 02-Jul-22
 */
@ExtensionImpl
public class PlainTextSpellcheckingStrategy extends SpellcheckingStrategy
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return PlainTextLanguage.INSTANCE;
	}
}
