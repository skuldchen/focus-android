package org.mozilla.focus.webkit.matcher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.webkit.matcher.Trie.WhiteListTrie;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class TrieTest {

    @Test
    public void findNode() throws Exception {
        final Trie trie = Trie.createRootNode();

        assertFalse(trie.contains("hello"));

        final Trie putNode = trie.put("hello");
        final Trie.TrieIterator foundNodes = trie.findNodes("hello");

        assertNotNull(putNode);
        assertTrue(trie.contains("hello"));
        // We've only inserted one matching domain, so it will be the first item (returned by next()).
        assertTrue(foundNodes.hasNext());
        assertEquals(putNode, foundNodes.next());
        assertFalse(foundNodes.hasNext());

        // Substring matching: doesn't happen (except for subdomains, we test those later)
        assertFalse(trie.contains("hell"));
        assertFalse(trie.contains("hellop"));

        trie.put("hellohello");

        // Ensure both old and new overlapping strings can still be found
        assertTrue(trie.contains("hello"));
        assertTrue(trie.contains("hellohello"));

        // These still don't match:
        assertFalse(trie.contains("hell"));
        assertFalse(trie.contains("hellop"));

        // Domain specific / partial domain tests:
        trie.put("foo.com");

        // Domain and subdomain can be found
        assertTrue(trie.contains("foo.com"));
        assertTrue(trie.contains("bar.foo.com"));
        // But other domains with some overlap don't match
        assertFalse(trie.contains("bar-foo.com"));
        assertFalse(trie.contains("oo.com"));
    }

    @Test
    public void testWhiteListTrie() {
        final WhiteListTrie trie;

        {
            final Trie whitelist = Trie.createRootNode();

            whitelist.put("abc");

            trie = WhiteListTrie.createRootNode();
            trie.putWhiteList("def", whitelist);
        }

        assertFalse(trie.contains("abc"));

        // In practice EntityList uses it's own search in order to cover all possible matching notes
        // (e.g. in case we have separate whitelists for mozilla.org and foo.mozilla.org), however
        // we don't need to test that here yet.
        final Trie.TrieIterator iterator = trie.findNodes("def");

        assertTrue(iterator.hasNext());
        final WhiteListTrie foundWhitelist = (WhiteListTrie) iterator.next();
        assertFalse(iterator.hasNext());

        assertNotNull(foundWhitelist);

        assertTrue(foundWhitelist.whitelist.contains("abc"));
        assertFalse(foundWhitelist.whitelist.contains("def"));
    }
}