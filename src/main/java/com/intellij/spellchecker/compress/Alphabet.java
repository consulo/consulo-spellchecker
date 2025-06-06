/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.spellchecker.compress;

import jakarta.annotation.Nonnull;

public final class Alphabet {

  private final char[] letters;
  private int lastIndexUsed = 0;
  private static final int MAX_INDEX = UnitBitSet.MAX_UNIT_VALUE;

  public char getLetter(int position) {
    return letters[position];
  }

  /*
    @param forceAdd - if set to true - letter will be added to the alphabet if not present yet
    @return index of the letter or -1 if letter was not found and could not be added (due to forceAdd property value)
  */

  public int getIndex(char letter, boolean forceAdd) {
    return getNextIndex(0, letter, forceAdd);
  }

  /*
   @param forceAdd - if set to true - letter will be added to the alphabet if not present yet
   @return index of the letter or -1 if letter was not found and could not be added (due to forceAdd property value)
  */
  public int getNextIndex(int startFrom, char letter, boolean forceAdd) {
    for (int i = startFrom; i <= lastIndexUsed; i++) {
      if (i == letters.length) return -1;
      if (letters[i] != 0 && letters[i] == letter) {
        return i;
      }
    }
    if (!forceAdd) {
      return -1;
    }
    return add(letter);
  }

  public int getLastIndexUsed() {
    return lastIndexUsed;
  }

  public int add(char c) {
    if(lastIndexUsed>=letters.length-1) return -1;
    lastIndexUsed++;
    letters[lastIndexUsed] = c;
    return lastIndexUsed;
  }


  Alphabet() {
    this(MAX_INDEX);
  }

  Alphabet(int maxIndex) {
    assert maxIndex <= MAX_INDEX : "alphabet is too long";
    letters = new char[maxIndex];
  }

  // TODO: this should be ONLY way to create it to sped up getIndex
  Alphabet(@Nonnull CharSequence alphabet) {
    this(alphabet.length() + 1);
    for (int i = 0; i < alphabet.length(); i++) {
      add(alphabet.charAt(i));
    }
  }
}
