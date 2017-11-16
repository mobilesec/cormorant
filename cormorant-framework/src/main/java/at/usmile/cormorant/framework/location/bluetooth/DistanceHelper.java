package at.usmile.cormorant.framework.location.bluetooth;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Uses AltBeacon’s algorithm for distance measuring
 */

public class DistanceHelper {
    private Map<UUID, List<DISTANCE>> distancesForUUIDMap = new HashMap<>();
    private final static int AVERAGE_COUNT = 15;

    public double calculateAccuracy(int txPower, int rssi) {
        if (rssi == 0) {
            return -1.0; // unknown
        }
        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }

    //Based on Motorola Z Play with ADVERTISE_TX_POWER_MEDIUM (-75 TxPower configured) and Nexus 6
    public DISTANCE estimateDistance(double accuracy) {
        if (accuracy == -1.0) {
            return DISTANCE.UNKNOWN;
        } else if (accuracy < 0.1) {
            return DISTANCE.CLOSE;
        } else if (accuracy < 1.5) {
            return DISTANCE.NEAR;
        } else if (accuracy < 4) {
            return DISTANCE.SAME_ROOM;
        } else {
            return DISTANCE.FAR;
        }
    }

    public DISTANCE averageDistance(DISTANCE distance, UUID uuid) {
        List<DISTANCE> distancesForUUID = distancesForUUIDMap.get(uuid);
        if(distancesForUUID == null) {
            distancesForUUID = new LinkedList<>();
            distancesForUUIDMap.put(uuid, distancesForUUID);
        }
        distancesForUUID.add(distance);

        //Find most common Distance in list
        if(distancesForUUID.size() >= AVERAGE_COUNT) {
            DISTANCE averageDistance = distancesForUUID.stream()
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .max(Comparator.comparing(Map.Entry::getValue)).get().getKey();
            distancesForUUID.clear();
            return averageDistance;
        } else return null;
    }

    public enum DISTANCE {
        UNKNOWN,    //no value
        CLOSE,      //<0,3m
        NEAR,       //>0,3m < 1m (?)
        SAME_ROOM,  // ?
        FAR         //>5m (?)
    }
}
