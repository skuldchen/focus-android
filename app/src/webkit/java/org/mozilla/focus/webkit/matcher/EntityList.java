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
        // We need to find all entries that match the site host: it's possible for multiple whitelists
        // to match a given domain, e.g. the whitelist can contain entries for both the root domain (bar.com)
        // and subdomains (foo.bar.com). If we're visiting *.foo.bar.com we need to check whether a resource is whitelisted
        // for either bar.com or foo.bar.com.
        final Trie.TrieIterator iterator = revHostTrie.findNodes(siteHost);

        while (iterator.hasNext()) {
            final WhiteListTrie node = (WhiteListTrie) iterator.next();

            if (node.whitelist.contains(resourceHost)) {
                return true;
            }
        }

        return false;
    }
}
