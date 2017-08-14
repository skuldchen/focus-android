/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import java.util.UUID;

public class Session {
    private final String uuid;
    private final MutableLiveData<String> url;
    private final MutableLiveData<Integer> progress;
    private final MutableLiveData<Boolean> secure;
    private final MutableLiveData<Boolean> loading;
    private final MutableLiveData<Integer> trackersBlocked;

    /* package */ Session(String url) {
        this.uuid = UUID.randomUUID().toString();

        this.url = new MutableLiveData<>();
        this.progress = new MutableLiveData<>();
        this.secure = new MutableLiveData<>();
        this.loading = new MutableLiveData<>();
        this.trackersBlocked = new MutableLiveData<>();

        this.url.setValue(url);
        this.progress.setValue(0);
        this.secure.setValue(false);
        this.loading.setValue(false);
        this.trackersBlocked.setValue(0);
    }

    public String getUUID() {
        return uuid;
    }

    /* package */ void setUrl(String url) {
        this.url.setValue(url);
    }

    public LiveData<String> getUrl() {
        return url;
    }

    /* package */ void setProgress(int progress) {
        this.progress.setValue(progress);
    }

    public LiveData<Integer> getProgress() {
        return progress;
    }

    /* package */ void setSecure(boolean secure) {
        this.secure.setValue(secure);
    }

    public LiveData<Boolean> getSecure() {
        return secure;
    }

    /* package */ void setLoading(boolean loading) {
        this.loading.setValue(loading);
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /* package */ void setTrackersBlocked(int trackersBlocked) {
        this.trackersBlocked.postValue(trackersBlocked);
    }

    public LiveData<Integer> getBlockedTrackers() {
        return trackersBlocked;
    }
}
