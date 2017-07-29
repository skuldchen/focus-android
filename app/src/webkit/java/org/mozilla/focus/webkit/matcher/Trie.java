/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.webkit.matcher;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.SparseArray;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A domain specific Trie implementation.
 *
 * Domains are stored in reversed format. e.g. if abd.de and efg.hi are stored, the root node
 * will point to e and i. This means that subdomains will end up being children of their root domain
 * (which is important since subdomains are considered to match root domains).
 * Clients of Trie do not need to care about this: domains should be inserted as a normal String (e.g. foo.com),
 * searches should also be performed with a normal string (foo.com, bar.foo.com, etc), Trie will
 * take care of the appropriate reversal.
 *
 * See also some online documentation:
 *
 * And a blog post on the topic (note: this blog post contains partially outdated information
 * regarding the implementation of Trie search).
 */
/* package-private */ class Trie {

    /**
     * Trie that adds storage for a whitelist (itself another trie) on each node.
     */
    public static class WhiteListTrie extends Trie {
        Trie whitelist = null;

        private WhiteListTrie(final char character, final WhiteListTrie parent) {
            super(character, parent);
        }

        @Override
        protected Trie createNode(final char character, final Trie parent) {
            return new WhiteListTrie(character, (WhiteListTrie) parent);
        }

        public static WhiteListTrie createRootNode() {
            return new WhiteListTrie(Character.MIN_VALUE, null);
        }

        /* Convenience method so that clients aren't forced to do their own casting. */
        public void putWhiteList(final String host, final Trie whitelist) {
            WhiteListTrie node = (WhiteListTrie) super.put(host);

            if (node.whitelist != null) {
                throw new IllegalStateException("Whitelist already set for node " + host);
            }

            node.whitelist = whitelist;
        }
    }

    public final SparseArray<Trie> children = new SparseArray<>();

    /**
     * Whether this node represents a valid domain. Should be true for the node that represents
     * the first character in a domain, i.e. the 'f' in foo.com.
     */
    public boolean isDomain = false;

    /**
     * Iterator that implements searching for the node representing a specific domain.
     *
     * Contains an iterative Trie search implementation, that is specific to domain searches (i.e. we assume
     * that the domain is stored in reverse format in the Trie, we therefore walk backwards along the
     * "input"/search string, and have special conditions for subdomains.
     *
     * Recursive non-static implementations are possible, but less efficient - moreover handling
     * the input String gets messy (we can substring which is inefficient, we can pass around string
     * offsets which gets messy, or we can have a String wrapper class which is convoluted). Earlier
     * implementations did in fact use substring() (which was slowest since Android substring creates
     * a copy), followed by a String wrapper taking care of offsets (which was significantly faster,
     * but still convoluted and hard to understand).
     *
     * Search performance is particularly important as this method is called ~3 times per webpage resource
     * (once per enabled blocklist). Webpages typically try to load tens of resources. Naive search
     * implementations can therefore impact perceived page load performance.
     *
     * See the following for more background on the evolution of our trie search:
     * - Why we use a Trie instead of iterating over a list of domains:
     * https://github.com/mozilla-mobile/focus-android/issues/123
     * - Why a naive recursive implementation is suboptimal:
     * https://github.com/mozilla-mobile/focus-android/issues/571
     */
    public static class TrieIterator implements Iterator<Trie> {
        private final @NonNull String searchTerm;

        /**
         * The next node that will be returned by next().
         *
         * The only time this does not point to a valid matching node (or null) is during initialisation,
         * when we point to the root of the Trie.
         */
        private @Nullable Trie node = null;

        /**
         * The next character after the character represented by node. I.e. the next character we
         * need to search for after calling next().
         */
        private int nextOffset;

        public TrieIterator(final @NonNull String searchTerm, final @NonNull Trie trie) {
            this.searchTerm = searchTerm;
            this.node = trie;
            this.nextOffset = 0;

            findAndUpdateNext();
        }

        @Override
        public boolean hasNext() {
            return node != null;
        }

        @Override
        public Trie next() {
            if (node == null) {
                throw new NoSuchElementException("No more matching nodes remaining");
            }

            final Trie current = node;

            findAndUpdateNext();

            return current;
        }

        /**
         * Attempts to find the next matching node, and updates node/nextOffset to match.
         */
        private void findAndUpdateNext() {
            if (TextUtils.isEmpty(searchTerm)) {
                node = null;
                return;
            }

            // length 5, offset max 4, length = 5,
            while (nextOffset < searchTerm.length()) {
                // Do a little calculation because we're traversing the string back to front. We could
                // reverse the string in advance but that wastes memory and time.
                final int characterPosition = searchTerm.length() - nextOffset - 1;
                final char character = searchTerm.charAt(characterPosition);

                node = node.children.get(character);

                if (node == null) {
                    return;
                }

                nextOffset++;

                final int nextCharacterPosition = characterPosition - 1;
                final boolean atLastCharacter = (characterPosition == 0);

                if (node.isDomain &&
                        (atLastCharacter || // Indicates we're at the end of the
                                searchTerm.charAt(nextCharacterPosition) == '.')) {
                    return;
                }
            }

            // Once we've reached the end of the string without any matches we're done. node points
            // to a non-terminating node that happens to match our string (which is thus NOT a domain match),
            // we need to clear it since node must only point to a valid matching node.
            node = null;
        }
    }

    /**
     * Find all entries that match a given hostname.
     *
     * @param host Hostname to search for.
     * @return All entries that match the requested hostname, including all superdomains. If you
     * search for foo.bar.com, nodes representing bar.com and foo.bar.com will be returned (assuming
     * that both bar.com and foo.bar.com have previously been inserted into the Trie). No ordering
     * is guaranteed, although this implementation will return shorter matches first (bar.com before foo.bar.com).
     */
    public TrieIterator findNodes(final String host) {
        return new TrieIterator(host, this);
    }

    /**
     * Query whether a given hostname matches any entries in this Trie. Will return true for partial
     * and complete matches. contains("foo.bar.com") will return true if any of "foo.bar.com", "bar.com",
     * or "com" are contained in the Trie.
     */
    public boolean contains(final String host) {
        final TrieIterator iterator = findNodes(host);

        return iterator.hasNext();
    }

    /**
     * Insert a given domain into the Trie.
     *
     * @param host Any domain name, e.g. "foo.bar.com".
     * @return The node that represents the inserted domain.
     */
    public Trie put(final String host) {
        return put(host, this);
    }

    /**
     * Insert a given domain into the Trie. Callers should supply a normal domain (e.g. "foo.bar.com"),
     * putChar() will take care of the appropriate domain reversal.
     */
    private static Trie put(final String host, final Trie root) {
        Trie node = root;

        for (int i = host.length() - 1; i >= 0; i--) {
            final char c = host.charAt(i);

            node = node.putChar(c);
        }

        node.isDomain = true;
        return node;
    }

    /**
     * Obtain the child node representing a specific character. A new node will be created if
     * necessary.
     */
    public Trie putChar(final char character) {
        final Trie existingChild = children.get(character);

        if (existingChild != null) {
            return existingChild;
        }

        final Trie newChild = createNode(character, this);

        children.put(character, newChild);

        return newChild;
    }

    private Trie(final char character, final Trie parent) {
        if (parent != null) {
            parent.children.put(character, this);
        }
    }

    public static Trie createRootNode() {
        return new Trie(Character.MIN_VALUE, null);
    }

    // Subclasses must override to provide their node implementation
    protected Trie createNode(final char character, final Trie parent) {
        return new Trie(character, parent);
    }
}
