package com.roguelike.resourcepack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResourcePackManagerTest {

    @AfterEach
    void resetCache() {
        ResourcePackManager.setCachedHashForTest(null);
    }

    @Test
    void deriveSha1UrlAppendsSha1Suffix() {
        assertEquals(
                "https://github.com/LIPiston/minerogue-resourcepack/releases/download/nightly/minerogue-resourcepack.zip.sha1",
                ResourcePackManager.deriveSha1Url("https://github.com/LIPiston/minerogue-resourcepack/releases/download/nightly/minerogue-resourcepack.zip"));
    }

    @Test
    void deriveSha1UrlPreservesQueryAndRequiresZipExtension() {
        assertEquals("https://example.com/pack.zip.sha1?token=abc",
                ResourcePackManager.deriveSha1Url("https://example.com/pack.zip?token=abc"));
        assertNull(ResourcePackManager.deriveSha1Url("https://example.com/pack.tar"));
        assertNull(ResourcePackManager.deriveSha1Url(null));
    }

    @Test
    void extractSha1AcceptsBareHashAndLowercases() {
        assertEquals("89005a1b098c4192ff89ccc8df13c828b12416c5",
                ResourcePackManager.extractSha1("89005A1B098C4192FF89CCC8DF13C828B12416C5"));
    }

    @Test
    void extractSha1IgnoresSurroundingNoise() {
        // sha1sum-style "hash  filename" lines are tolerated.
        assertEquals("89005a1b098c4192ff89ccc8df13c828b12416c5",
                ResourcePackManager.extractSha1("89005a1b098c4192ff89ccc8df13c828b12416c5  minerogue-resourcepack.zip"));
        assertNull(ResourcePackManager.extractSha1("not a hash"));
        assertNull(ResourcePackManager.extractSha1(""));
        assertNull(ResourcePackManager.extractSha1(null));
    }

    @Test
    void resolveHashPrefersExplicitConfigValue() {
        ResourcePackManager.setCachedHashForTest("89005a1b098c4192ff89ccc8df13c828b12416c5");
        assertEquals("abcdef0123456789abcdef0123456789abcdef01",
                ResourcePackManager.resolveHash("abcdef0123456789abcdef0123456789abcdef01"));
    }

    @Test
    void resolveHashFallsBackToCachedWhenConfigBlank() {
        ResourcePackManager.setCachedHashForTest("89005a1b098c4192ff89ccc8df13c828b12416c5");
        assertEquals("89005a1b098c4192ff89ccc8df13c828b12416c5",
                ResourcePackManager.resolveHash(""));
        assertEquals("89005a1b098c4192ff89ccc8df13c828b12416c5",
                ResourcePackManager.resolveHash(null));
    }

    @Test
    void resolveHashReturnsNullWhenNothingAvailable() {
        assertNull(ResourcePackManager.resolveHash(""));
        assertNull(ResourcePackManager.resolveHash(null));
    }
}
