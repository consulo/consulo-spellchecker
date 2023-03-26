/**
 * @author VISTALL
 * @since 02-Apr-22
 */
open module com.intellij.spellchecker {
	// TODO remove this dependency in future
	requires java.desktop;

	requires consulo.ide.api;

	exports com.intellij.spellchecker;
	exports com.intellij.spellchecker.compress;
	exports com.intellij.spellchecker.dictionary;
	exports com.intellij.spellchecker.engine;
	exports com.intellij.spellchecker.generator;
	exports com.intellij.spellchecker.inspections;
	exports com.intellij.spellchecker.quickfixes;
	exports com.intellij.spellchecker.settings;
	exports com.intellij.spellchecker.state;
	exports com.intellij.spellchecker.ui;
	exports com.intellij.spellchecker.util;
	exports consulo.spellchecker.icon;
}