package org.mozilla.focus.webkit.matcher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.webkit.matcher.Trie.WhiteListTrie;
import org.mozilla.focus.webkit.matcher.util.FocusString;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class TrieTest {

    @Test
    public void findNode() throws Exception {
        final Trie trie = Trie.createRootNode();

        assertNull(trie.findNode("hello"));

        final Trie putNode = trie.put(FocusString.create("hello").reverse());
        final Trie foundNode = trie.findNode("hello");

        assertNotNull(putNode);
        assertNotNull(foundNode);
        assertEquals(putNode, foundNode);

        // Substring matching: doesn't happen (except for subdomains, we test those later)
        assertNull(trie.findNode("hell"));
        assertNull(trie.findNode("hellop"));

        trie.put(FocusString.create("hellohello").reverse());

        // Ensure both old and new overlapping strings can still be found
        assertNotNull(trie.findNode("hello"));
        assertNotNull(trie.findNode("hellohello"));

        // These still don't match:
        assertNull(trie.findNode("hell"));
        assertNull(trie.findNode("hellop"));

        // Domain specific / partial domain tests:
        trie.put(FocusString.create("foo.com").reverse());

        // Domain and subdomain can be found
        assertNotNull(trie.findNode("foo.com"));
        assertNotNull(trie.findNode("bar.foo.com"));
        // But other domains with some overlap don't match
        assertNull(trie.findNode("bar-foo.com"));
        assertNull(trie.findNode("oo.com"));
    }

    @Test
    public void testWhiteListTrie() {
        final WhiteListTrie trie;

        {
            final Trie whitelist = Trie.createRootNode();

            whitelist.put(FocusString.create("abc").reverse());

            trie = WhiteListTrie.createRootNode();
            trie.putWhiteList(FocusString.create("def").reverse(), whitelist);
        }

        assertNull(trie.findNode("abc"));

        // In practice EntityList uses it's own search in order to cover all possible matching notes
        // (e.g. in case we have separate whitelists for mozilla.org and foo.mozilla.org), however
        // we don't need to test that here yet.
        final WhiteListTrie foundWhitelist = (WhiteListTrie) trie.findNode("def");
        assertNotNull(foundWhitelist);

        assertNotNull(foundWhitelist.whitelist.findNode("abc"));
    }
}