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
package com.intellij.spellchecker.compress;

import com.intellij.spellchecker.DefaultBundledDictionariesProvider;
import com.intellij.spellchecker.StreamLoader;
import com.intellij.spellchecker.dictionary.Dictionary;
import com.intellij.spellchecker.dictionary.Loader;
import com.intellij.spellchecker.engine.Transformation;
import consulo.util.collection.Sets;
import consulo.util.io.FileUtil;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SuppressWarnings({"JUnitTestCaseWithNonTrivialConstructors"})
public class DictionaryTest {
    private Dictionary dictionary;

    private final Map<String, Integer> sizes = Map.of(
        PROGRAMMING_DIC, 1000,
        ENGLISH_DIC, 140000
    );
    private final Map<String, Integer> times = Map.of(
        PROGRAMMING_DIC, 1000,
        ENGLISH_DIC, 50000
    );

    private static final String PROGRAMMING_DIC = "programming.dic";
    private static final String ENGLISH_DIC = "english.dic";

//    @Test
//    public void testDictionary() throws IOException {
//        String[] names = {PROGRAMMING_DIC, ENGLISH_DIC};
//        for (String name : names) {
//            loadDictionaryTest(name, sizes.get(name));
//            loadHalfDictionaryTest(name, 50000);
//        }
//    }

    @Test
    public void testDictionaryLoadedFully() {
        //cleanupDictionary();
        Transformation transform = new Transformation();
        CompressedDictionary dictionary = CompressedDictionary.create(englishLoader(), transform);

        Set<String> onDisk = new HashSet<>();
        englishLoader().load(s -> {
            assert s != null;
            String t = transform.transform(s);
            if (t == null) {
                return;
            }
            onDisk.add(t);
        });

        List<String> odList = new ArrayList<>(onDisk);
        Collections.sort(odList);
        List<String> loaded = new ArrayList<>(dictionary.getWords());
        Collections.sort(loaded);

        assertEquals(odList, loaded);
    }

    public void cleanupDictionary() {
        Set<String> onDisk = Sets.newHashSet(FileUtil.PATH_HASHING_STRATEGY);
        englishLoader().load(s -> {
            assert s != null;
            onDisk.add(s);
        });

        List<String> odList = new ArrayList<>(onDisk);
        Collections.sort(odList);

        File file = new File("C:\\Work\\Idea\\community\\spellchecker\\src\\com\\intellij\\spellchecker\\english.2");
        try {
            FileUtil.writeToFile(file, StringUtil.join(odList, "\n"));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static StreamLoader englishLoader() {
        return new StreamLoader(DefaultBundledDictionariesProvider.class.getResourceAsStream(ENGLISH_DIC), ENGLISH_DIC);
    }

//    public void loadDictionaryTest(@Nonnull String name, int wordCount) throws IOException {
//        Transformation transform = new Transformation();
//        PlatformTestUtil.startPerformanceTest(
//            "load dictionary",
//            times.get(name),
//            (ThrowableRunnable) () -> dictionary = CompressedDictionary
//                .create(new StreamLoader(DefaultBundledDictionariesProvider.class.getResourceAsStream(name), name), transform)
//        ).cpuBound().assertTiming();
//
//        Set<String> wordsToStoreAndCheck = createWordSets(name, 50000, 1).getFirst();
//        PlatformTestUtil.startPerformanceTest(
//            "words contain",
//            2000,
//            (ThrowableRunnable) () -> {
//                for (String s : wordsToStoreAndCheck) {
//                    assertTrue(dictionary.contains(s));
//                }
//            }
//        ).cpuBound().assertTiming();
//    }

    private static Loader createLoader(final Set<String> words) {
        return new Loader() {
            @Override
            public void load(@Nonnull Consumer<String> consumer) {
                for (String word : words) {
                    consumer.accept(word);
                }
            }

            @Override
            public String getName() {
                return "test";
            }
        };
    }

    @SuppressWarnings({"unchecked"})
    private static Couple<Set<String>> createWordSets(String name, final int maxCount, final int mod) {
        Loader loader = new StreamLoader(DefaultBundledDictionariesProvider.class.getResourceAsStream(name), name);
        final Set<String> wordsToStore = new HashSet<>();
        final Set<String> wordsToCheck = new HashSet<>();
        final Transformation transform = new Transformation();
        loader.load(new Consumer<>() {
            private int counter = 0;

            @Override
            public void accept(String s) {
                if (counter > maxCount) {
                    return;
                }
                String transformed = transform.transform(s);
                if (transformed != null) {

                    if (counter % mod == 0) {
                        wordsToStore.add(transformed);
                    }
                    else {
                        wordsToCheck.add(transformed);
                    }
                    counter++;
                }
            }
        });

        return Couple.of(wordsToStore, wordsToCheck);
    }

    public static void loadHalfDictionaryTest(String name, int maxCount) {
        Couple<Set<String>> sets = createWordSets(name, maxCount, 2);
        Loader loader = createLoader(sets.getFirst());
        CompressedDictionary dictionary = CompressedDictionary.create(loader, new Transformation());
        for (String s : sets.getSecond()) {
            if (!sets.getFirst().contains(s)) {
                assertFalse(dictionary.contains(s), s);
            }
        }
    }
}
