package com.intellij.spellchecker.compress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncodeAndCompressTest {
    @Test
    public void testEncodeAndCompress() {
        Encoder encoder = new Encoder();
        String word = "example";
        UnitBitSet bs = encoder.encode(word, true);
        byte[] compressed = bs.pack();
        String decompressed = UnitBitSet.decode(compressed, encoder.getAlphabet());
        assertEquals(word,decompressed);
        String restored = encoder.decode(compressed);
        assertEquals(word,restored);
    }
}
