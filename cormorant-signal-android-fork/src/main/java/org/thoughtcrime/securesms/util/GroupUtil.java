package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientModifiedListener;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.whispersystems.signalservice.internal.push.SignalServiceProtos.GroupContext;

public class GroupUtil {

  private static final String ENCODED_SIGNAL_GROUP_PREFIX = "__textsecure_group__!";
  private static final String ENCODED_MMS_GROUP_PREFIX    = "__signal_mms_group__!";
  private static final String TAG                  = GroupUtil.class.getSimpleName();

  public static String getEncodedId(byte[] groupId, boolean mms) {
    return (mms ? ENCODED_MMS_GROUP_PREFIX  : ENCODED_SIGNAL_GROUP_PREFIX) + Hex.toStringCondensed(groupId);
  }

  public static byte[] getDecodedId(String groupId) throws IOException {
    if (!isEncodedGroup(groupId)) {
      throw new IOException("Invalid encoding");
    }

    return Hex.fromStringCondensed(groupId.split("!", 2)[1]);
  }

  public static boolean isEncodedGroup(@NonNull String groupId) {
    return groupId.startsWith(ENCODED_SIGNAL_GROUP_PREFIX) || groupId.startsWith(ENCODED_MMS_GROUP_PREFIX);
  }

  public static boolean isMmsGroup(@NonNull String groupId) {
    return groupId.startsWith(ENCODED_MMS_GROUP_PREFIX);
  }

  public static @NonNull GroupDescription getDescription(@NonNull Context context, @Nullable String encodedGroup) {
    if (encodedGroup == null) {
      return new GroupDescription(context, null);
    }

    try {
      GroupContext  groupContext = GroupContext.parseFrom(Base64.decode(encodedGroup));
      return new GroupDescription(context, groupContext);
    } catch (IOException e) {
      Log.w(TAG, e);
      return new GroupDescription(context, null);
    }
  }



  public static class GroupDescription {

    @NonNull  private final Context         context;
    @Nullable private final GroupContext    groupContext;
    @Nullable private final List<Recipient> members;

    public GroupDescription(@NonNull Context context, @Nullable GroupContext groupContext) {
      this.context      = context.getApplicationContext();
      this.groupContext = groupContext;

      if (groupContext == null || groupContext.getMembersList().isEmpty()) {
        this.members = null;
      } else {
        this.members = new LinkedList<>();

        for (String member : groupContext.getMembersList()) {
          this.members.add(Recipient.from(context, Address.fromSerialized(member), true));
        }
      }
    }

    public String toString(Recipient sender) {
      StringBuilder description = new StringBuilder();
      description.append( sender.toShortString() + " updated the group.");

      if (groupContext == null) {
        return description.toString();
      }

      String title = groupContext.getName();

      if (members != null) {
        description.append("\n");
        description.append(    members.size() + "joined the group " + toString(members));
      }

      if (title != null && !title.trim().isEmpty()) {
        if (members != null) description.append(" ");
        else                 description.append("\n");
        description.append("Group name is now " + title);
      }

      return description.toString();
    }

    public void addListener(RecipientModifiedListener listener) {
      if (this.members != null) {
        for (Recipient member : this.members) {
          member.addListener(listener);
        }
      }
    }

    private String toString(List<Recipient> recipients) {
      String result = "";

      for (int i=0;i<recipients.size();i++) {
        result += recipients.get(i).toShortString();

      if (i != recipients.size() -1 )
        result += ", ";
    }

    return result;
    }
  }
}
