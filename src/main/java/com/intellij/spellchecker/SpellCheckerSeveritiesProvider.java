/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 17-Jun-2009
 */
package com.intellij.spellchecker;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeveritiesProvider;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.List;

@ExtensionImpl
public class SpellCheckerSeveritiesProvider extends SeveritiesProvider
{
	private static final TextAttributesKey TYPO_KEY = TextAttributesKey.createTextAttributesKey("TYPO");
	public static final HighlightSeverity TYPO = new HighlightSeverity("TYPO", HighlightSeverity.INFORMATION.myVal + 5);

	static class TYPO extends HighlightInfoType.HighlightInfoTypeImpl implements HighlightInfoType.Iconable
	{
		public TYPO()
		{
			super(TYPO, TYPO_KEY);
		}

		@Nonnull
		@Override
		public Image getIcon()
		{
			return AllIcons.General.InspectionsTypos;
		}
	}

	@Override
	@Nonnull
	public List<HighlightInfoType> getSeveritiesHighlightInfoTypes()
	{
		return List.of(new TYPO());
	}

	@Override
	public ColorValue getTrafficRendererColor(@Nonnull TextAttributes textAttributes)
	{
		return textAttributes.getErrorStripeColor();
	}

	@Override
	public boolean isGotoBySeverityEnabled(HighlightSeverity minSeverity)
	{
		return TYPO != minSeverity;
	}
}