/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.webkit.matcher;


import android.net.Uri;
import android.text.TextUtils;

import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.webkit.matcher.Trie.WhiteListTrie;

/* package-private */ class EntityList {

    private WhiteListTrie rootNode;

    public EntityList() {
        rootNode = WhiteListTrie.createRootNode();
    }

    public void putWhiteList(final String host, final Trie whitelist) {
        rootNode.putWhiteList(host, whitelist);
    }

    public boolean isWhiteListed(final Uri site, final Uri resource) {
        if (TextUtils.isEmpty(site.getHost()) ||
                TextUtils.isEmpty(resource.getHost()) ||
                site.getScheme().equals("data")) {
            return false;
        }

        if (UrlUtils.isPermittedResourceProtocol(resource.getScheme()) &&
                UrlUtils.isSupportedProtocol(site.getScheme())) {
            return isWhiteListed(site.getHost(), resource.getHost(), rootNode);
        } else {
            // This might be some imaginary/custom protocol: theguardian.com loads
            // things like "nielsenwebid://nuid/999" and/or sets an iFrame URL to that:
            return false;
        }
    }

    private boolean isWhiteListed(final String siteHost, final String resourceHost, final Trie revHostTrie) {
        // We could use Trie.findNode(), however it's possible to have subdomain specific whitelists - hence we need
        // to manually search for every matching node (in effect we reimplement findNode, but continue past the
        // first matching Node whereas findNode() simply returns the first matching Node).
        // TODO: we could reimplement findNode() to return a lazy iterator (and add appropriate tests : ) )

        int offset = 0;
        WhiteListTrie node = (WhiteListTrie) revHostTrie;

        while (offset < siteHost.length()) {
            if (node == null) {
                return false;
            }

            final int currentCharPosition = siteHost.length() - 1 - offset;
            final char currentChar = siteHost.charAt(currentCharPosition);

            // Match achieved - and we're at a domain boundary. This is important, because
            // we don't want to return on partial domain matches. (E.g. if the trie node is bar.com,
            // and the search string is foo-bar.com, we shouldn't match. foo.bar.com should however match.)
            if (node.terminator &&
                    node.whitelist != null &&
                    currentChar == '.' &&
                    node.whitelist.findNode(resourceHost) != null) {
                return true;
            }

            node = (WhiteListTrie) node.children.get(currentChar);
            offset++;
        }

        return node.terminator &&
                node.whitelist != null &&
                node.whitelist.findNode(resourceHost) != null;
    }
}
