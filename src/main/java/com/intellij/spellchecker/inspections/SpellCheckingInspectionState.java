package com.intellij.spellchecker.inspections;

import com.intellij.spellchecker.util.SpellCheckerBundle;
import consulo.configurable.ConfigurableBuilder;
import consulo.configurable.UnnamedConfigurable;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.localize.LocalizeValue;
import consulo.util.xml.serializer.XmlSerializerUtil;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 11/03/2023
 */
public class SpellCheckingInspectionState implements InspectionToolState<SpellCheckingInspectionState> {
    public boolean processCode = true;
    public boolean processLiterals = true;
    public boolean processComments = true;

    @Nullable
    @Override
    public SpellCheckingInspectionState getState() {
        return this;
    }

    @Override
    public void loadState(SpellCheckingInspectionState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Nullable
    @Override
    public UnnamedConfigurable createConfigurable() {
        ConfigurableBuilder builder = ConfigurableBuilder.newBuilder();
        builder.checkBox(LocalizeValue.localizeTODO(SpellCheckerBundle.message("process.code")), () -> processCode, it -> processCode = it);
        builder.checkBox(LocalizeValue.localizeTODO(SpellCheckerBundle.message("process.literals")), () -> processLiterals, it -> processLiterals = it);
        builder.checkBox(LocalizeValue.localizeTODO(SpellCheckerBundle.message("process.comments")), () -> processComments, it -> processComments = it);

        return builder.buildUnnamed();
    }
}
