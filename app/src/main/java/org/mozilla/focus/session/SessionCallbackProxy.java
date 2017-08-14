/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.mozilla.focus.web.Download;
import org.mozilla.focus.web.IWebView;

public class SessionCallbackProxy implements IWebView.Callback{
    private final Session session;
    private final IWebView.Callback delegate;

    public SessionCallbackProxy(Session session, IWebView.Callback delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    @Override
    public void onPageStarted(String url) {
        session.setLoading(true);
        session.setSecure(false);

        session.setUrl(url);
    }

    @Override
    public void onPageFinished(boolean isSecure) {
        session.setLoading(false);
        session.setSecure(isSecure);
    }

    @Override
    public void onProgress(int progress) {
        session.setProgress(progress);
    }

    @Override
    public void onURLChanged(String url) {
        session.setUrl(url);
    }

    @Override
    public boolean handleExternalUrl(String url) {
        return delegate.handleExternalUrl(url); // TODO: Replace
    }

    @Override
    public void onDownloadStart(Download download) {
        delegate.onDownloadStart(download); // TODO: Replace
    }

    @Override
    public void onLongPress(IWebView.HitTarget hitTarget) {
        delegate.onLongPress(hitTarget); // TODO: Replace
    }

    @Override
    public void onEnterFullScreen(@NonNull IWebView.FullscreenCallback callback, @Nullable View view) {
        delegate.onEnterFullScreen(callback, view); // TODO: Replace
    }

    @Override
    public void onExitFullScreen() {
        delegate.onExitFullScreen(); // TODO: Replace
    }

    @Override
    public void countBlockedTracker() {
        //noinspection ConstantConditions - The value is never null
        session.setTrackersBlocked(session.getBlockedTrackers().getValue() + 1);
    }

    @Override
    public void resetBlockedTrackers() {
        session.setTrackersBlocked(0);
    }
}
