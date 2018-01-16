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
package at.usmile.cormorant.framework.messaging;

import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.thoughtcrime.securesms.crypto.storage.SignalParameter;
import org.thoughtcrime.securesms.crypto.storage.SignalProtocolStoreImpl;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.signalservice.api.SignalServiceMessagePipe;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.crypto.SignalServiceCipher;
import org.whispersystems.signalservice.api.messages.SignalServiceContent;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import at.usmile.cormorant.framework.group.GroupChallengeRequest;
import at.usmile.cormorant.framework.group.GroupChallengeResponse;
import at.usmile.cormorant.framework.group.GroupUpdateMessage;
import at.usmile.cormorant.framework.lock.DeviceLockCommand;

public class SignalListenTask implements Runnable {

    private final static String MESSAGE_SPLIT_CHAR = "!";

    private final static String LOG_TAG = SignalListenTask.class.getSimpleName();

    private volatile boolean shutdown;

    private final Gson gson = new GsonBuilder().create();

    private SignalParameter signalParameter;

    private SignalServiceMessageReceiver messageReceiver;

    private SignalProtocolStoreImpl signalProtocolStore;

    private ListMultimap<CormorantMessage.TYPE, CormorantMessageConsumer> messageListeners;
    private Set<IConnectionListener> connectionListeners;

    public SignalListenTask(SignalParameter signalParameter, SignalServiceMessageReceiver messageReceiver, SignalProtocolStoreImpl signalProtocolStore, ListMultimap<CormorantMessage.TYPE, CormorantMessageConsumer> messageListeners, Set<IConnectionListener> connectionListeners ) {
        Preconditions.checkNotNull(signalParameter);
        Preconditions.checkNotNull(messageReceiver);
        Preconditions.checkNotNull(signalProtocolStore);
        Preconditions.checkNotNull(signalParameter);
        Preconditions.checkNotNull(connectionListeners);

        this.signalParameter = signalParameter;
        this.messageReceiver = messageReceiver;
        this.signalProtocolStore = signalProtocolStore;
        this.messageListeners = messageListeners;
        this.connectionListeners = connectionListeners;
    }

    public void shutdown() {
        this.shutdown = true;
    }

    @Override
    public void run() {
        Log.w(LOG_TAG, "Waiting for websocket state change....");

        Log.w(LOG_TAG, "Making websocket connection....");
        final SignalServiceMessagePipe messagePipe = messageReceiver.createMessagePipe();

        connectionListeners.forEach(l -> l.connected(signalParameter.getUser()));

        try {
            while (!shutdown) {
                try {
                    Log.w(LOG_TAG, "Reading message...");
                    messagePipe.read(1, TimeUnit.MINUTES, this::onMessage);
                } catch (TimeoutException e) {
                    Log.w(LOG_TAG, "Application level read timeout...");
                } catch (InvalidVersionException e) {
                    Log.w(LOG_TAG, e);
                }

                Log.w(LOG_TAG, "Looping...");
            }
        } catch (Throwable e) {
            Log.w(LOG_TAG, e);
        } finally {
            Log.w(LOG_TAG, "Shutting down pipe...");
            messagePipe.shutdown();
        }
    }

    private void onMessage(SignalServiceEnvelope envelope) {
        Log.w(LOG_TAG, "Retrieved envelope! " + envelope.getSource());

        SignalServiceCipher cipher = new SignalServiceCipher(new SignalServiceAddress(signalParameter.getUser().toString()), signalProtocolStore);
        try {
            Log.w(LOG_TAG, "Decrypting");
            SignalServiceContent message = cipher.decrypt(envelope);
            Log.w(LOG_TAG, "Received message: " + message.getDataMessage().get());
            Log.w(LOG_TAG, "Received message: " + message.getDataMessage().get().getBody().get());

            final CormorantMessage cormorantMessage = SignalListenTask.this.parseMessage(message.getDataMessage().get().getBody().get());
            if (cormorantMessage == null) return;
            List<CormorantMessageConsumer> messageConsumers = messageListeners.get(cormorantMessage.getType());
            if (messageConsumers != null) {
                for (CormorantMessageConsumer eachConsumer : messageConsumers) {
                    eachConsumer.handleMessage(cormorantMessage, envelope.getSource());
                }
            }
        } catch (InvalidMessageException e) {
            Log.w(LOG_TAG, e);
        } catch (Exception e) {
            Log.w(LOG_TAG, e);
            throw new RuntimeException(e);
        }
    }

    private CormorantMessage parseMessage(String message) {
        Log.w(LOG_TAG,"parseMessage: " +  message);

        String[] parts = message.split(MESSAGE_SPLIT_CHAR);
        if (CormorantMessage.CLASS.GROUP_CHALLENGE_REQUEST.toString().equals(parts[0])) {
            return gson.fromJson(parts[1], GroupChallengeRequest.class);
        } else if (CormorantMessage.CLASS.GROUP_CHALLENGE_RESPONSE.toString().equals(parts[0])) {
            return gson.fromJson(parts[1], GroupChallengeResponse.class);
        } else if (CormorantMessage.CLASS.GROUP_UPDATE.toString().equals(parts[0])) {
            return gson.fromJson(parts[1], GroupUpdateMessage.class);
        } else if (CormorantMessage.CLASS.DEVICE_LOCK_COMMAND.toString().equals(parts[0])) {
            return gson.fromJson(parts[1], DeviceLockCommand.class);
        } else {
            Log.w(LOG_TAG, "ClassType unknown" + parts[0]);
            return null;
        }
    }
}
