/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.mozilla.focus.utils.SafeIntent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    // TODO: Maybe use something better than a plain list?
    private MutableLiveData<List<Session>> sessions;
    private String currentSessionUUID; // TODO: Keep UUID or session object?

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    private SessionManager() {
        sessions = new MutableLiveData<>();
        sessions.setValue(Collections.unmodifiableList(Collections.<Session>emptyList()));
    }

    // TODO: How to add firstrun here? -- Or: How to let MainActivity handle that?
    public void handleIntent(final SafeIntent intent, final Bundle savedInstanceState) {
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            return; // TODO: Document
        }

        if (savedInstanceState != null) {
            // We are restoring a previous session - No need to handle this Intent.
            return;
        }

        final String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            final String url = intent.getDataString();

            // TODO: Custom Tab Config
            createSession(url);
        }
    }

    public Session getCurrentSession() {
        if (currentSessionUUID == null) {
            throw new IllegalAccessError("There's no active session");
        }

        for (Session session : sessions.getValue()) {
            if (currentSessionUUID.equals(session.getUUID())) {
                return session;
            }
        }

        throw new IllegalAccessError("There's no active session with the current UUID");
    }

    public Session getSessionByUUID(@NonNull String uuid) {
        for (Session session : sessions.getValue()) {
            if (uuid.equals(session.getUUID())) {
                return session;
            }
        }

        throw new IllegalAccessError("There's no active session with this UUID");
    }

    public LiveData<List<Session>> getSessions() {
        return sessions;
    }

    public Session createSession(@NonNull String url) {
        final Session session = new Session(url);

        // TODO: The newly created session is always the current one until we actually support multiple.
        currentSessionUUID = session.getUUID();

        // TODO: Currently we only have one session at all times.
        final List<Session> sessions = new ArrayList<>();
        sessions.add(session);

        this.sessions.setValue(Collections.unmodifiableList(sessions));

        return session;
    }

    public void removeSessions() {
        sessions.setValue(Collections.unmodifiableList(Collections.<Session>emptyList()));
    }
}
