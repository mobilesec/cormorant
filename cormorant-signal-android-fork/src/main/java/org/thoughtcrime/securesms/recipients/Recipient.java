/*
 * Copyright (C) 2011 Whisper Systems
 * Copyright (C) 2013 - 2017 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.recipients;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.function.Consumer;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.color.MaterialColor;
import org.thoughtcrime.securesms.contacts.avatars.ContactColors;
import org.thoughtcrime.securesms.contacts.avatars.ContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.FallbackContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.GeneratedContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.GroupRecordContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.ProfileContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.SystemContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.TransparentContactPhoto;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.GroupDatabase;
import org.thoughtcrime.securesms.database.RecipientDatabase.RecipientSettings;
import org.thoughtcrime.securesms.database.RecipientDatabase.RegisteredState;
import org.thoughtcrime.securesms.database.RecipientDatabase.VibrateState;
import org.thoughtcrime.securesms.recipients.RecipientProvider.RecipientDetails;
import org.thoughtcrime.securesms.util.FutureTaskListener;
import org.thoughtcrime.securesms.util.ListenableFutureTask;
import org.thoughtcrime.securesms.util.Util;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;

public class Recipient implements RecipientModifiedListener {

  private static final String            TAG      = Recipient.class.getSimpleName();
  private static final RecipientProvider provider = new RecipientProvider();

  private final Set<RecipientModifiedListener> listeners = Collections.newSetFromMap(new WeakHashMap<RecipientModifiedListener, Boolean>());

  private final @NonNull Address address;
  private final @NonNull List<Recipient> participants = new LinkedList<>();

  private @Nullable String  name;
  private @Nullable String  customLabel;
  private           boolean resolving;

  private @Nullable Uri                  systemContactPhoto;
  private @Nullable Long                 groupAvatarId;
  private           Uri                  contactUri;
  private @Nullable Uri                  ringtone              = null;
  private           long                 mutedUntil            = 0;
  private           boolean              blocked               = false;
  private           VibrateState         vibrate               = VibrateState.DEFAULT;
  private           int                  expireMessages        = 0;
  private           Optional<Integer>    defaultSubscriptionId = Optional.absent();
  private @NonNull  RegisteredState      registered            = RegisteredState.UNKNOWN;

  private @Nullable MaterialColor  color;
  private           boolean        seenInviteReminder;
  private @Nullable byte[]         profileKey;
  private @Nullable String         profileName;
  private @Nullable String         profileAvatar;
  private           boolean        profileSharing;
  private           boolean        isSystemContact;


  @SuppressWarnings("ConstantConditions")
  public static @NonNull Recipient from(@NonNull Context context, @NonNull Address address, boolean asynchronous) {
    if (address == null) throw new AssertionError(address);
    return provider.getRecipient(context, address, Optional.absent(), Optional.absent(), asynchronous);
  }

  @SuppressWarnings("ConstantConditions")
  public static @NonNull Recipient from(@NonNull Context context, @NonNull Address address, @NonNull Optional<RecipientSettings> settings, @NonNull Optional<GroupDatabase.GroupRecord> groupRecord, boolean asynchronous) {
    if (address == null) throw new AssertionError(address);
    return provider.getRecipient(context, address, settings, groupRecord, asynchronous);
  }

  public static void applyCached(@NonNull Address address, Consumer<Recipient> consumer) {
    Optional<Recipient> recipient = provider.getCached(address);
    if (recipient.isPresent()) consumer.accept(recipient.get());
  }

  Recipient(@NonNull  Address address,
            @Nullable Recipient stale,
            @NonNull  Optional<RecipientDetails> details,
            @NonNull  ListenableFutureTask<RecipientDetails> future)
  {
    this.address              = address;
    this.color                = null;
    this.resolving            = true;

    if (stale != null) {
      this.name                  = stale.name;
      this.contactUri            = stale.contactUri;
      this.systemContactPhoto    = stale.systemContactPhoto;
      this.groupAvatarId         = stale.groupAvatarId;
      this.color                 = stale.color;
      this.customLabel           = stale.customLabel;
      this.ringtone              = stale.ringtone;
      this.mutedUntil            = stale.mutedUntil;
      this.blocked               = stale.blocked;
      this.vibrate               = stale.vibrate;
      this.expireMessages        = stale.expireMessages;
      this.seenInviteReminder    = stale.seenInviteReminder;
      this.defaultSubscriptionId = stale.defaultSubscriptionId;
      this.registered            = stale.registered;
      this.profileKey            = stale.profileKey;
      this.profileName           = stale.profileName;
      this.profileAvatar         = stale.profileAvatar;
      this.profileSharing        = stale.profileSharing;
      this.isSystemContact       = stale.isSystemContact;
      this.participants.clear();
      this.participants.addAll(stale.participants);
    }

    if (details.isPresent()) {
      this.name                  = details.get().name;
      this.systemContactPhoto    = details.get().systemContactPhoto;
      this.groupAvatarId         = details.get().groupAvatarId;
      this.color                 = details.get().color;
      this.ringtone              = details.get().ringtone;
      this.mutedUntil            = details.get().mutedUntil;
      this.blocked               = details.get().blocked;
      this.vibrate               = details.get().vibrateState;
      this.expireMessages        = details.get().expireMessages;
      this.seenInviteReminder    = details.get().seenInviteReminder;
      this.defaultSubscriptionId = details.get().defaultSubscriptionId;
      this.registered            = details.get().registered;
      this.profileKey            = details.get().profileKey;
      this.profileName           = details.get().profileName;
      this.profileAvatar         = details.get().profileAvatar;
      this.profileSharing        = details.get().profileSharing;
      this.isSystemContact       = details.get().systemContact;
      this.participants.clear();
      this.participants.addAll(details.get().participants);
    }

    future.addListener(new FutureTaskListener<RecipientDetails>() {
      @Override
      public void onSuccess(RecipientDetails result) {
        if (result != null) {
          synchronized (Recipient.this) {
            Recipient.this.name                  = result.name;
            Recipient.this.contactUri            = result.contactUri;
            Recipient.this.systemContactPhoto    = result.systemContactPhoto;
            Recipient.this.groupAvatarId         = result.groupAvatarId;
            Recipient.this.color                 = result.color;
            Recipient.this.customLabel           = result.customLabel;
            Recipient.this.ringtone              = result.ringtone;
            Recipient.this.mutedUntil            = result.mutedUntil;
            Recipient.this.blocked               = result.blocked;
            Recipient.this.vibrate               = result.vibrateState;
            Recipient.this.expireMessages        = result.expireMessages;
            Recipient.this.seenInviteReminder    = result.seenInviteReminder;
            Recipient.this.defaultSubscriptionId = result.defaultSubscriptionId;
            Recipient.this.registered            = result.registered;
            Recipient.this.profileKey            = result.profileKey;
            Recipient.this.profileName           = result.profileName;
            Recipient.this.profileAvatar         = result.profileAvatar;
            Recipient.this.profileSharing        = result.profileSharing;
            Recipient.this.profileName           = result.profileName;
            Recipient.this.isSystemContact       = result.systemContact;

            Recipient.this.participants.clear();
            Recipient.this.participants.addAll(result.participants);
            Recipient.this.resolving = false;

            if (!listeners.isEmpty()) {
              for (Recipient recipient : participants) recipient.addListener(Recipient.this);
            }

            Recipient.this.notifyAll();
          }

          notifyListeners();
        }
      }

      @Override
      public void onFailure(ExecutionException error) {
        Log.w(TAG, error);
      }
    });
  }

  Recipient(@NonNull Address address, @NonNull RecipientDetails details) {
    this.address               = address;
    this.contactUri            = details.contactUri;
    this.name                  = details.name;
    this.systemContactPhoto    = details.systemContactPhoto;
    this.groupAvatarId         = details.groupAvatarId;
    this.color                 = details.color;
    this.customLabel           = details.customLabel;
    this.ringtone              = details.ringtone;
    this.mutedUntil            = details.mutedUntil;
    this.blocked               = details.blocked;
    this.vibrate               = details.vibrateState;
    this.expireMessages        = details.expireMessages;
    this.seenInviteReminder    = details.seenInviteReminder;
    this.defaultSubscriptionId = details.defaultSubscriptionId;
    this.registered            = details.registered;
    this.profileKey            = details.profileKey;
    this.profileName           = details.profileName;
    this.profileAvatar         = details.profileAvatar;
    this.profileSharing        = details.profileSharing;
    this.isSystemContact       = details.systemContact;
    this.participants.addAll(details.participants);
    this.resolving    = false;
  }

  public synchronized @Nullable Uri getContactUri() {
    return this.contactUri;
  }

  public void setContactUri(@Nullable Uri contactUri) {
    boolean notify = false;

    synchronized (this) {
      if (!Util.equals(contactUri, this.contactUri)) {
        this.contactUri = contactUri;
        notify = true;
      }
    }

    if (notify) notifyListeners();
  }

  public synchronized @Nullable String getName() {
    if (this.name == null && isMmsGroupRecipient()) {
      List<String> names = new LinkedList<>();

      for (Recipient recipient : participants) {
        names.add(recipient.toShortString());
      }

      return Util.join(names, ", ");
    }

    return this.name;
  }

  public void setName(@Nullable String name) {
    boolean notify = false;

    synchronized (this) {
      if (!Util.equals(this.name, name)) {
        this.name = name;
        notify = true;
      }
    }

    if (notify) notifyListeners();
  }

  public synchronized @NonNull MaterialColor getColor() {
    if      (isGroupRecipient()) return MaterialColor.GROUP;
    else if (color != null)      return color;
    else if (name != null)       return ContactColors.generateFor(name);
    else                         return ContactColors.UNKNOWN_COLOR;
  }

  public void setColor(@NonNull MaterialColor color) {
    synchronized (this) {
      this.color = color;
    }

    notifyListeners();
  }

  public @NonNull Address getAddress() {
    return address;
  }

  public synchronized @Nullable String getCustomLabel() {
    return customLabel;
  }

  public void setCustomLabel(@Nullable String customLabel) {
    boolean notify = false;

    synchronized (this) {
      if (!Util.equals(customLabel, this.customLabel)) {
        this.customLabel = customLabel;
        notify = true;
      }
    }

    if (notify) notifyListeners();
  }

  public synchronized Optional<Integer> getDefaultSubscriptionId() {
    return defaultSubscriptionId;
  }

  public void setDefaultSubscriptionId(Optional<Integer> defaultSubscriptionId) {
    synchronized (this) {
      this.defaultSubscriptionId = defaultSubscriptionId;
    }

    notifyListeners();
  }

  public synchronized @Nullable String getProfileName() {
    return profileName;
  }

  public void setProfileName(@Nullable String profileName) {
    synchronized (this) {
      this.profileName = profileName;
    }

    notifyListeners();
  }

  public synchronized @Nullable String getProfileAvatar() {
    return profileAvatar;
  }

  public void setProfileAvatar(@Nullable String profileAvatar) {
    synchronized (this) {
      this.profileAvatar = profileAvatar;
    }

    notifyListeners();
  }

  public synchronized boolean isProfileSharing() {
    return profileSharing;
  }

  public void setProfileSharing(boolean value) {
    synchronized (this) {
      this.profileSharing = value;
    }

    notifyListeners();
  }

  public boolean isGroupRecipient() {
    return address.isGroup();
  }

  public boolean isMmsGroupRecipient() {
    return address.isMmsGroup();
  }

  public boolean isPushGroupRecipient() {
    return address.isGroup() && !address.isMmsGroup();
  }

  public @NonNull synchronized List<Recipient> getParticipants() {
    return new LinkedList<>(participants);
  }

  public void setParticipants(@NonNull List<Recipient> participants) {
    synchronized (this) {
      this.participants.clear();
      this.participants.addAll(participants);
    }

    notifyListeners();
  }

  public synchronized void addListener(RecipientModifiedListener listener) {
    if (listeners.isEmpty()) {
      for (Recipient recipient : participants) recipient.addListener(this);
    }
    listeners.add(listener);
  }

  public synchronized void removeListener(RecipientModifiedListener listener) {
    listeners.remove(listener);

    if (listeners.isEmpty()) {
      for (Recipient recipient : participants) recipient.removeListener(this);
    }
  }

  public synchronized String toShortString() {
    return (getName() == null ? address.serialize() : getName());
  }

  public synchronized @NonNull Drawable getFallbackContactPhotoDrawable(Context context, boolean inverted) {
    return getFallbackContactPhoto().asDrawable(context, getColor().toConversationColor(context), inverted);
  }

  public synchronized @NonNull FallbackContactPhoto getFallbackContactPhoto() {
    if      (isResolving())            return new TransparentContactPhoto();
    else if (isGroupRecipient())       return new ResourceContactPhoto(R.drawable.ic_group_white_24dp, R.drawable.ic_group_large);
    else if (!TextUtils.isEmpty(name)) return new GeneratedContactPhoto(name);
    else                               return new GeneratedContactPhoto("#");
  }

  public synchronized @Nullable ContactPhoto getContactPhoto() {
    if      (isGroupRecipient() && groupAvatarId != null) return new GroupRecordContactPhoto(address, groupAvatarId);
    else if (systemContactPhoto != null)                  return new SystemContactPhoto(address, systemContactPhoto, 0);
    else if (profileAvatar != null)                       return new ProfileContactPhoto(address, profileAvatar);
    else                                                  return null;
  }

  public void setSystemContactPhoto(@Nullable Uri systemContactPhoto) {
    boolean notify = false;

    synchronized (this) {
      if (!Util.equals(systemContactPhoto, this.systemContactPhoto)) {
        this.systemContactPhoto = systemContactPhoto;
        notify = true;
      }
    }

    if (notify) notifyListeners();
  }

  public void setGroupAvatarId(@Nullable Long groupAvatarId) {
    boolean notify = false;

    synchronized (this) {
      if (!Util.equals(this.groupAvatarId, groupAvatarId)) {
        this.groupAvatarId = groupAvatarId;
        notify = true;
      }
    }

    if (notify) notifyListeners();
  }

  public synchronized @Nullable Uri getRingtone() {
    if (ringtone != null && ringtone.getScheme() != null && ringtone.getScheme().startsWith("file")) {
      return null;
    }

    return ringtone;
  }

  public void setRingtone(@Nullable Uri ringtone) {
    synchronized (this) {
      this.ringtone = ringtone;
    }

    notifyListeners();
  }

  public synchronized boolean isMuted() {
    return System.currentTimeMillis() <= mutedUntil;
  }

  public void setMuted(long mutedUntil) {
    synchronized (this) {
      this.mutedUntil = mutedUntil;
    }

    notifyListeners();
  }

  public synchronized boolean isBlocked() {
    return blocked;
  }

  public void setBlocked(boolean blocked) {
    synchronized (this) {
      this.blocked = blocked;
    }

    notifyListeners();
  }

  public synchronized VibrateState getVibrate() {
    return vibrate;
  }

  public void setVibrate(VibrateState vibrate) {
    synchronized (this) {
      this.vibrate = vibrate;
    }

    notifyListeners();
  }

  public synchronized int getExpireMessages() {
    return expireMessages;
  }

  public void setExpireMessages(int expireMessages) {
    synchronized (this) {
      this.expireMessages = expireMessages;
    }

    notifyListeners();
  }

  public synchronized boolean hasSeenInviteReminder() {
    return seenInviteReminder;
  }

  public void setHasSeenInviteReminder(boolean value) {
    synchronized (this) {
      this.seenInviteReminder = value;
    }

    notifyListeners();
  }

  public synchronized RegisteredState getRegistered() {
    if      (isPushGroupRecipient()) return RegisteredState.REGISTERED;
    else if (isMmsGroupRecipient())  return RegisteredState.NOT_REGISTERED;

    return registered;
  }

  public void setRegistered(@NonNull RegisteredState value) {
    boolean notify = false;

    synchronized (this) {
      if (this.registered != value) {
        this.registered = value;
        notify = true;
      }
    }

    if (notify) notifyListeners();
  }

  public synchronized @Nullable byte[] getProfileKey() {
    return profileKey;
  }

  public void setProfileKey(@Nullable byte[] profileKey) {
    synchronized (this) {
      this.profileKey = profileKey;
    }

    notifyListeners();
  }

  public synchronized boolean isSystemContact() {
    return isSystemContact;
  }

  public synchronized Recipient resolve() {
    while (resolving) Util.wait(this, 0);
    return this;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof Recipient)) return false;

    Recipient that = (Recipient) o;

    return this.address.equals(that.address);
  }

  @Override
  public int hashCode() {
    return this.address.hashCode();
  }

  private void notifyListeners() {
    Set<RecipientModifiedListener> localListeners;

    synchronized (this) {
      localListeners = new HashSet<>(listeners);
    }

    for (RecipientModifiedListener listener : localListeners)
      listener.onModified(this);
  }

  @Override
  public void onModified(Recipient recipient) {
    notifyListeners();
  }

  public synchronized boolean isResolving() {
    return resolving;
  }


}
