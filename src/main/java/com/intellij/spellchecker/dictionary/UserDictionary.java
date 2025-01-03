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
package com.intellij.spellchecker.dictionary;

import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class UserDictionary implements EditableDictionary {
  private final String name;

  @Nonnull
  private final Set<String> words = new HashSet<>();

  public UserDictionary(@Nonnull String name) {
    this.name = name;
  }

  @Nonnull
  @Override
  public String getName() {
    return name;
  }

  @Override
  @Nullable
  public Boolean contains(@Nonnull String word) {
    boolean contains = words.contains(word);
    if(contains) return true;
    return null;
  }

  @Override
  public int size() {
    return words.size();
  }

  @Override
  @Nullable
  public Set<String> getWords() {
    return words;
  }

  @Override
  @Nullable
  public Set<String> getEditableWords() {
    return words;
  }

  @Override
  public void clear() {
    words.clear();
  }


  @Override
  public void addToDictionary(String word) {
    if (word == null) {
      return;
    }
    words.add(word);
  }

  @Override
  public void removeFromDictionary(String word) {
    if (word == null) {
      return;
    }
    words.remove(word);
  }

  @Override
  public void replaceAll(@Nullable Collection<String> words) {
    clear();
    addToDictionary(words);
  }

  @Override
  public void addToDictionary(@Nullable Collection<String> words) {
    if (words == null || words.isEmpty()) {
      return;
    }
    for (String word : words) {
      addToDictionary(word);
    }
  }

  @Override
  public boolean isEmpty() {
    return words.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserDictionary that = (UserDictionary)o;

    return name.equals(that.name);

  }

  @Override
  public void traverse(@Nonnull final Consumer<String> consumer) {
    for (String word : words) {
      consumer.accept(word);
    }
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @NonNls
  @Override
  public String toString() {
    return "UserDictionary{" + "name='" + name + '\'' + ", words.count=" + words.size() + '}';
  }
}
