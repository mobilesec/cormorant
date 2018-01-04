package org.thoughtcrime.securesms.crypto.storage;

import android.location.Address;

import org.whispersystems.libsignal.IdentityKey;

public class IdentityRecord {

    public enum VerifiedStatus {
        DEFAULT, VERIFIED, UNVERIFIED;

        public int toInt() {
            if (this == DEFAULT) return 0;
            else if (this == VERIFIED) return 1;
            else if (this == UNVERIFIED) return 2;
            else throw new AssertionError();
        }

        public static VerifiedStatus forState(int state) {
            if (state == 0) return DEFAULT;
            else if (state == 1) return VERIFIED;
            else if (state == 2) return UNVERIFIED;
            else throw new AssertionError("No such state: " + state);
        }
    }

    private final Address address;
    private final IdentityKey identitykey;
    private final VerifiedStatus verifiedStatus;
    private final boolean firstUse;
    private final long timestamp;
    private final boolean nonblockingApproval;

    private IdentityRecord(Address address,
                           IdentityKey identitykey, VerifiedStatus verifiedStatus,
                           boolean firstUse, long timestamp, boolean nonblockingApproval) {
        this.address = address;
        this.identitykey = identitykey;
        this.verifiedStatus = verifiedStatus;
        this.firstUse = firstUse;
        this.timestamp = timestamp;
        this.nonblockingApproval = nonblockingApproval;
    }

    public Address getAddress() {
        return address;
    }

    public IdentityKey getIdentityKey() {
        return identitykey;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public VerifiedStatus getVerifiedStatus() {
        return verifiedStatus;
    }

    public boolean isApprovedNonBlocking() {
        return nonblockingApproval;
    }

    public boolean isFirstUse() {
        return firstUse;
    }

    @Override
    public String toString() {
        return "{address: " + address + ", identityKey: " + identitykey + ", verifiedStatus: " + verifiedStatus + ", firstUse: " + firstUse + "}";
    }

}