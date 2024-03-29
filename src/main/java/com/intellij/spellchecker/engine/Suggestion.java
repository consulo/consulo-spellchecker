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
package com.intellij.spellchecker.engine;

import consulo.util.lang.StringUtil;

public class Suggestion implements Comparable{
  private final String word;
  private final int metrics;

  public Suggestion(String word, int metrics) {
    this.word = word;
    this.metrics = metrics;
  }

  public String getWord() {
    return word;
  }


  public int getMetrics() {
    return metrics;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Suggestion result = (Suggestion)o;

    if (metrics != result.metrics) return false;
    if (word != null ? !word.equals(result.word) : result.word != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = word != null ? word.hashCode() : 0;
    result = 31 * result + metrics;
    return result;
  }


  @Override
  public int compareTo(Object o) {
    if (!(o instanceof Suggestion)) throw new IllegalArgumentException();
    Suggestion r = (Suggestion)o;
    int c = Integer.valueOf(getMetrics()).compareTo(r.getMetrics());
    if (c !=0) return c;
    return StringUtil.compare(word, r.word, true);
  }

  @Override
  public String toString() {
    return word + " : " + metrics;
  }
}



