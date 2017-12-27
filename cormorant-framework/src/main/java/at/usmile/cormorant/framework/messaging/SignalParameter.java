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

import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.signalservice.api.push.TrustStore;
import org.whispersystems.signalservice.internal.configuration.SignalCdnUrl;
import org.whispersystems.signalservice.internal.configuration.SignalServiceConfiguration;
import org.whispersystems.signalservice.internal.configuration.SignalServiceUrl;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by hintzed on 12/21/17.
 */

public class SignalParameter {

    private final static String LOG_TAG = SignalMessagingService.class.getSimpleName();

    private static final String URL = "https://cormorant.hintze-it.de/";

    private static final String PREF_SIGNAL_USER = "signalUser";
    private static final String PREF_SIGNAL_PASSWORD = "signalPassword";
    private static final String PREF_SIGNAL_KEY = "signalKey";
    private static final String PREF_SIGNAL_REG_ID = "signalRegId";
    private static final String PREF_SIGNAL_PRE_KEYS = "signalPreKey";

    private IdentityKeyPair identityKey;
    private List<PreKeyRecord> oneTimePreKeys;

    private String user;
    private String password;
    private String signalingKey;
    private int registrationId;
    private boolean isNew;

    public static boolean isPresent(SharedPreferences preferences) {
        return preferences.contains(PREF_SIGNAL_USER);
    }

    public static SignalServiceConfiguration getServiceConfiguration(TrustStore trustStore) {
        return new SignalServiceConfiguration(new SignalServiceUrl[]{new SignalServiceUrl(URL, trustStore)}, new SignalCdnUrl[]{});
    }

    public static SignalParameter init() {
        SignalParameter parameter = new SignalParameter();

        parameter.user = UUID.randomUUID().toString();
        parameter.password = getSecret(50);
        parameter.signalingKey = getSecret(52);
        parameter.registrationId = generateRandomInstallId();
        parameter.oneTimePreKeys = KeyHelper.generatePreKeys(0, 100);
        parameter.isNew = true;

        Log.i(LOG_TAG, "Generating new signal parameter for user " + parameter.user);

        return parameter;
    }

    public static SignalParameter load(SharedPreferences preferences) {
        SignalParameter parameter = new SignalParameter();

        parameter.user = preferences.getString(PREF_SIGNAL_USER, null);
        parameter.password = preferences.getString(PREF_SIGNAL_PASSWORD, null);
        parameter.signalingKey = preferences.getString(PREF_SIGNAL_KEY, null);
        parameter.registrationId = preferences.getInt(PREF_SIGNAL_REG_ID, -1);
        parameter.oneTimePreKeys = fromString(preferences.getString(PREF_SIGNAL_PRE_KEYS, null));

        return parameter;
    }

    public void save(SharedPreferences preferences) {
        preferences.edit()
                .putString(PREF_SIGNAL_USER, user)
                .putString(PREF_SIGNAL_PASSWORD, password)
                .putString(PREF_SIGNAL_KEY, signalingKey)
                .putInt(PREF_SIGNAL_REG_ID, registrationId)
                .putString(PREF_SIGNAL_PRE_KEYS, asString(oneTimePreKeys))
                .commit();
    }

    private static String asString(List<PreKeyRecord> oneTimePreKey) {
        return oneTimePreKey.stream().map(k -> k.serialize()).map(b -> Base64.encodeToString(b, 0)).collect(Collectors.joining("|"));
    }

    private static List<PreKeyRecord> fromString(String string) {
        List<PreKeyRecord> list = new ArrayList<>();

        Arrays.stream(string.split("|")).map(s -> Base64.decode(s, 0)).forEach(b -> {
            try {
                list.add(new PreKeyRecord(b));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return list;

    }

    private static int generateRandomInstallId() {
        return (int) Math.random() * Integer.MAX_VALUE;
    }

    private static byte[] getSecretBytes(int size) {
        byte[] secret = new byte[size];
        getSecureRandom().nextBytes(secret);
        return secret;
    }

    private static String getSecret(int size) {
        return Base64.encodeToString(getSecretBytes(size), 0);
    }

    private static SecureRandom getSecureRandom() {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public String getUser() {
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
}
