/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.spellchecker.ui;

import com.intellij.spellchecker.inspections.SpellCheckingInspection;
import consulo.application.Application;
import consulo.codeEditor.EditorEx;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileWrapper;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.intention.IntentionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.ui.SimpleEditorCustomization;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.collection.Maps;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Allows to enforce editors to use/don't use spell checking ignoring user-defined spelling inspection settings.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since Aug 20, 2010 3:54:42 PM
 */
public class SpellCheckingEditorCustomization extends SimpleEditorCustomization
{

	public static final SpellCheckingEditorCustomization ENABLED = new SpellCheckingEditorCustomization(true);
	public static final SpellCheckingEditorCustomization DISABLED = new SpellCheckingEditorCustomization(false);

	private static final Map<String, LocalInspectionToolWrapper> SPELL_CHECK_TOOLS = new HashMap<String, LocalInspectionToolWrapper>();
	private static final boolean READY = init();

	@Nonnull
	public static SpellCheckingEditorCustomization getInstance(boolean enabled)
	{
		return enabled ? ENABLED : DISABLED;
	}

	private SpellCheckingEditorCustomization(boolean enabled)
	{
		super(enabled);
	}

	@SuppressWarnings({"unchecked"})
	private static boolean init()
	{
		// It's assumed that default spell checking inspection settings are just fine for processing all types of data.
		// Please perform corresponding settings tuning if that assumption is broken at future.

		Class<LocalInspectionTool>[] inspectionClasses = (Class<LocalInspectionTool>[]) new Class<?>[]{SpellCheckingInspection.class};
		for(Class<LocalInspectionTool> inspectionClass : inspectionClasses)
		{
			try
			{
				LocalInspectionTool tool = Application.get().getUnbindedInstance(inspectionClass);
				SPELL_CHECK_TOOLS.put(tool.getShortName(), new LocalInspectionToolWrapper(tool));
			}
			catch(Throwable e)
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public void customize(@Nonnull EditorEx editor)
	{
		boolean apply = isEnabled();

		if(!READY)
		{
			return;
		}

		Project project = editor.getProject();
		if(project == null)
		{
			return;
		}

		PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
		if(file == null)
		{
			return;
		}

		Function<InspectionProfile, InspectionProfileWrapper> strategy = file.getUserData(InspectionProfileWrapper.CUSTOMIZATION_KEY);
		if(strategy == null)
		{
			file.putUserData(InspectionProfileWrapper.CUSTOMIZATION_KEY, strategy = new MyInspectionProfileStrategy());
		}

		if(!(strategy instanceof MyInspectionProfileStrategy))
		{
			return;
		}

		((MyInspectionProfileStrategy) strategy).setUseSpellCheck(apply);

		if(apply)
		{
			editor.putUserData(IntentionManager.SHOW_INTENTION_OPTIONS_KEY, false);
		}

		// Update representation.
		DaemonCodeAnalyzer analyzer = DaemonCodeAnalyzer.getInstance(project);
		analyzer.restart(file);
	}

	private static class MyInspectionProfileStrategy implements Function<InspectionProfile, InspectionProfileWrapper>
	{
		private final Map<InspectionProfile, MyInspectionProfileWrapper> myWrappers = Maps.newWeakHashMap();
		private boolean myUseSpellCheck;

		@Override
		public InspectionProfileWrapper apply(InspectionProfile delegate)
		{
			if(!READY)
			{
				return new InspectionProfileWrapper(delegate);
			}
			MyInspectionProfileWrapper wrapper = myWrappers.get(delegate);
			if(wrapper == null)
			{
				myWrappers.put(delegate, wrapper = new MyInspectionProfileWrapper(delegate));
			}
			wrapper.setUseSpellCheck(myUseSpellCheck);
			return wrapper;
		}

		public void setUseSpellCheck(boolean useSpellCheck)
		{
			myUseSpellCheck = useSpellCheck;
		}
	}

	private static class MyInspectionProfileWrapper extends InspectionProfileWrapper
	{
		private boolean myUseSpellCheck;

		MyInspectionProfileWrapper(InspectionProfile delegate)
		{
			super(delegate);
		}

		@Override
		public boolean isToolEnabled(HighlightDisplayKey key, PsiElement element)
		{
			return SPELL_CHECK_TOOLS.containsKey(key.toString()) && myUseSpellCheck;
		}

		public void setUseSpellCheck(boolean useSpellCheck)
		{
			myUseSpellCheck = useSpellCheck;
		}
	}
}
