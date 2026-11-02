/*
 * Copyright 2013-2026 consulo.io
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
package com.intellij.spellchecker;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

import java.io.InputStream;
import java.util.function.BiConsumer;

/**
 * English dictionaries of the Simple Spellchecker engine. Read only while Simple Spellchecker is the active engine.
 * <p>
 * Each dictionary is a plain text file, one word per line. The provider opens the streams itself, so resources of its own module
 * are accessible. The consumer closes the stream.
 * <p>
 * Accepted words valid for every engine (keywords, library names) belong to
 * {@link consulo.language.spellchecker.editor.dictionary.BundledDictionaryProvider} instead.
 *
 * @author VISTALL
 * @since 2026-11-01
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SimpleDictionaryProvider {
    void loadDictionaries(BiConsumer<String, InputStream> consumer);
}
