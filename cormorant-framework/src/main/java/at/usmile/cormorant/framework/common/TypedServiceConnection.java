/**
 * Copyright 2016 - 2017
 *
 * Daniel Hintze <daniel.hintze@fhdw.de>
 * Sebastian Scholz <sebastian.scholz@fhdw.de>
 * Rainhard D. Findling <rainhard.findling@fh-hagenberg.at>
 * Muhammad Muaaz <muhammad.muaaz@usmile.at>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.usmile.cormorant.framework.common;

import android.app.Service;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class TypedServiceConnection<T extends Service> implements ServiceConnection {

    private T service;
    private boolean serviceBound = false;

    @Override
    public final void onServiceConnected(ComponentName className, IBinder service) {
        TypedServiceBinder<T> binder = (TypedServiceBinder<T>) service;
        this.service = binder.getService();
        this.serviceBound = true;

        onServiceConnected(this.service);
    }

    @Override
    public final void onServiceDisconnected(ComponentName componentName) {
        this.serviceBound = false;

        onServiceDisconnected(service);
    }

    public boolean isBound() {
        return serviceBound;
    }

    public T get() {
        return service;
    }

    /**
     * Callback, overwrite if needed
     */
    public void onServiceConnected(T service) {
    }

    /**
     * Callback, overwrite if needed
     */
    public void onServiceDisconnected(T service) {
    }
}