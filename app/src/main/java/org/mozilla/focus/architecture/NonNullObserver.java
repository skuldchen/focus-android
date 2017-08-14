package org.mozilla.focus.architecture;

import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

// TODO: Replace with a LiveData implementation that never returns null?
public abstract class NonNullObserver<T> implements Observer<T> {
    public abstract void onValueChanged(@NonNull T t);

    @Override
    public final void onChanged(@Nullable T value) {
        if (value == null) {
            return;
        }

        onValueChanged(value);
    }
}
