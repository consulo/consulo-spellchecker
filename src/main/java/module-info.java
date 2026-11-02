/**
 * @author VISTALL
 * @since 2022-04-02
 */
module com.intellij.spellchecker {
    requires transitive consulo.language.spellchecker.api;
    requires consulo.language.spellchecker.editor.api;
    requires consulo.language.editor.api;
    requires consulo.language.editor.refactoring.api;

    exports com.intellij.spellchecker;
    exports com.intellij.spellchecker.compress;
    exports com.intellij.spellchecker.dictionary;
    exports com.intellij.spellchecker.engine;
    exports com.intellij.spellchecker.generator;
    exports consulo.spellchecker.localize;
}
