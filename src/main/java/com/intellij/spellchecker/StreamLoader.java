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
package com.intellij.spellchecker;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.annotation.Nonnull;

import com.intellij.spellchecker.dictionary.Loader;
import com.intellij.util.Consumer;
import consulo.logging.Logger;

public class StreamLoader implements Loader {

  private static final Logger LOG = Logger.getInstance("#com.intellij.spellchecker.StreamLoader");
  private static final String ENCODING = "UTF-8";

  private final InputStream stream;
  private final String name;

  public StreamLoader(InputStream stream, String name) {
    this.stream = stream;
    this.name=name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void load(@Nonnull Consumer<String> consumer) {
    DataInputStream in = new DataInputStream(stream);
    BufferedReader br = null;

    try {
      br = new BufferedReader(new InputStreamReader(in, ENCODING));
      String strLine;
      while ((strLine = br.readLine()) != null) {
        consumer.consume(strLine);
      }
    }
    catch (Exception e) {
      LOG.error(e);
    }
    finally {
      try {
        br.close();
      }
      catch (IOException ignored) {

      }
    }
  }

}

