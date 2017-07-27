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

        assertNull(trie.findFirstNode("hello"));

        final Trie putNode = trie.put("hello");
        final Trie foundNode = trie.findFirstNode("hello");

        assertNotNull(putNode);
        assertNotNull(foundNode);
        assertEquals(putNode, foundNode);

        // Substring matching: doesn't happen (except for subdomains, we test those later)
        assertNull(trie.findFirstNode("hell"));
        assertNull(trie.findFirstNode("hellop"));

        trie.put("hellohello");

        // Ensure both old and new overlapping strings can still be found
        assertNotNull(trie.findFirstNode("hello"));
        assertNotNull(trie.findFirstNode("hellohello"));

        // These still don't match:
        assertNull(trie.findFirstNode("hell"));
        assertNull(trie.findFirstNode("hellop"));

        // Domain specific / partial domain tests:
        trie.put("foo.com");

        // Domain and subdomain can be found
        assertNotNull(trie.findFirstNode("foo.com"));
        assertNotNull(trie.findFirstNode("bar.foo.com"));
        // But other domains with some overlap don't match
        assertNull(trie.findFirstNode("bar-foo.com"));
        assertNull(trie.findFirstNode("oo.com"));
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

        assertNull(trie.findFirstNode("abc"));

        // In practice EntityList uses it's own search in order to cover all possible matching notes
        // (e.g. in case we have separate whitelists for mozilla.org and foo.mozilla.org), however
        // we don't need to test that here yet.
        final WhiteListTrie foundWhitelist = (WhiteListTrie) trie.findFirstNode("def");
        assertNotNull(foundWhitelist);

        assertNotNull(foundWhitelist.whitelist.findFirstNode("abc"));
    }
}