/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.spellchecker.util;

import consulo.application.util.function.Processor;
import consulo.util.io.FileUtil;

import java.io.File;
import java.util.function.Consumer;

@SuppressWarnings({"UtilityClassWithoutPrivateConstructor"})
public class SPFileUtil {

  public static void processFilesRecursively(final String rootPath, final Consumer<String> consumer){
    final File rootFile = new File(rootPath);
    if (rootFile.exists() && rootFile.isDirectory()){
      FileUtil.processFilesRecursively(rootFile, new Processor<File>() {
        public boolean process(final File file) {
          if (!file.isDirectory()){
            final String path = file.getPath();
            if (path.endsWith(".dic")){
              consumer.accept(path);
            }
          }
          return true;
        }
      });
    }
  }
}
