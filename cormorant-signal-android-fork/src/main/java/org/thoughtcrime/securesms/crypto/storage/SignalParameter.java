/**
 * Copyright 2016 - 2017
 * <p>
 * Daniel Hintze <daniel.hintze@fhdw.de>
 * Sebastian Scholz <sebastian.scholz@fhdw.de>
 * Rainhard D. Findling <rainhard.findling@fh-hagenberg.at>
 * Muhammad Muaaz <muhammad.muaaz@usmile.at>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thoughtcrime.securesms.crypto.storage;

import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.signalservice.api.push.TrustStore;
import org.whispersystems.signalservice.internal.configuration.SignalCdnUrl;
import org.whispersystems.signalservice.internal.configuration.SignalServiceConfiguration;
import org.whispersystems.signalservice.internal.configuration.SignalServiceUrl;
import org.whispersystems.signalservice.internal.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by hintzed on 12/21/17.
 */

public class SignalParameter {

    private final static String LOG_TAG = SignalParameter.class.getSimpleName();

    private static final String URL = "https://cormorant.hintze-it.de";

    public static final String PREFERENCE_NAME = "cormorant_x";

    private static final String PREF_SIGNAL_USER = "signalUser";
    private static final String PREF_SIGNAL_PASSWORD = "signalPassword";
    private static final String PREF_SIGNAL_KEY = "signalKey";
    private static final String PREF_SIGNAL_REG_ID = "signalRegId";
    private static final String PREF_SIGNAL_PRE_KEYS = "signalPreKey";
    private static final String PREF_SIGNAL_IDENTITY_KEY = "signalIdentityKey";
    private static final String PREF_SIGNAL_ACCOUNT_CREATED = "signalAccountCreated";

    private IdentityKeyPair identityKey;
    private List<PreKeyRecord> oneTimePreKeys;

    private UUID user;
    private String password;
    private String signalingKey;
    private int registrationId;
    private boolean isNew;
    private String gcmId;
    private boolean accountCreated;

    public static boolean isPresent(SharedPreferences preferences) {
        return preferences.getBoolean(PREF_SIGNAL_ACCOUNT_CREATED, false);
    }

    public static SignalServiceConfiguration getServiceConfiguration(TrustStore trustStore) {
        return new SignalServiceConfiguration(new SignalServiceUrl[]{new SignalServiceUrl(URL, trustStore)}, new SignalCdnUrl[]{});
    }

    public static SignalParameter init() {
        SignalParameter parameter = new SignalParameter();

        parameter.user = UUID.randomUUID();
        parameter.password = Util.getSecret(10);
        parameter.signalingKey = Util.getSecret(52);
        parameter.registrationId = generateRandomInstallId();
        parameter.identityKey = KeyHelper.generateIdentityKeyPair();
        parameter.oneTimePreKeys = KeyHelper.generatePreKeys(0, 1000);
        parameter.isNew = true;

        Log.i(LOG_TAG, "Generating new signal parameter for user " + parameter.user);

        return parameter;
    }

    public static SignalParameter load(SharedPreferences preferences) {
        SignalParameter parameter = new SignalParameter();

        try {
            parameter.user = UUID.fromString(preferences.getString(PREF_SIGNAL_USER, null));
            parameter.password = preferences.getString(PREF_SIGNAL_PASSWORD, null);
            parameter.signalingKey = preferences.getString(PREF_SIGNAL_KEY, null);
            parameter.registrationId = preferences.getInt(PREF_SIGNAL_REG_ID, -1);
            parameter.identityKey = new IdentityKeyPair(Base64.decode(preferences.getString(PREF_SIGNAL_IDENTITY_KEY, null), 0));
            parameter.oneTimePreKeys = listFromString(preferences.getString(PREF_SIGNAL_PRE_KEYS, null));
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        return parameter;
    }

    public void save(SharedPreferences preferences) {
        preferences.edit()
                .putString(PREF_SIGNAL_USER, user.toString())
                .putString(PREF_SIGNAL_PASSWORD, password)
                .putString(PREF_SIGNAL_KEY, signalingKey)
                .putInt(PREF_SIGNAL_REG_ID, registrationId)
                .putString(PREF_SIGNAL_IDENTITY_KEY, Base64.encodeToString(identityKey.serialize(), Base64.NO_WRAP))
                .putString(PREF_SIGNAL_PRE_KEYS, asString(oneTimePreKeys))
                .putBoolean(PREF_SIGNAL_ACCOUNT_CREATED, accountCreated)
                .commit();
    }

    private static String asString(List<PreKeyRecord> oneTimePreKey) {
        return oneTimePreKey.stream().map(k -> k.serialize()).map(b -> Base64.encodeToString(b, Base64.NO_WRAP)).collect(Collectors.joining(","));
    }

    private static List<PreKeyRecord> listFromString(String string) {
        List<PreKeyRecord> list = new ArrayList<>();

        Arrays.stream(string.split(",")).map(s -> Base64.decode(s, Base64.NO_WRAP)).forEach(b -> {
            try {
                list.add(new PreKeyRecord(b));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return list;

    }

    public void setAccountCreated() {
        this.accountCreated = accountCreated;
    }

    private static int generateRandomInstallId() {
        return (int) Math.random() * Integer.MAX_VALUE;
    }

    public UUID getUser() {
        return user;
    }

    public IdentityKeyPair getIdentityKey() {
        return identityKey;
    }

    public List<PreKeyRecord> getOneTimePreKeys() {
        return oneTimePreKeys;
    }

    public String getPassword() {
        return password;
    }

    public String getSignalingKey() {
        return signalingKey;
    }

    public int getRegistrationId() {
        return registrationId;
    }

    public boolean isNew() {
        return isNew;
    }

    public String getGcmId() {
        return gcmId;
    }

    @Override
    public String toString() {
        return "SignalParameter{" +
                "identityKey=" + identityKey +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", signalingKey='" + signalingKey + '\'' +
                ", registrationId=" + registrationId +
                ", isNew=" + isNew +
                '}';
    }

    public void setGcmId(String gcmId) {
        this.gcmId = gcmId;
    }
}
