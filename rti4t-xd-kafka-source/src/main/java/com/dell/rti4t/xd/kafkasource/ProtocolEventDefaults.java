package com.dell.rti4t.xd.kafkasource;

import java.util.Map;

public class ProtocolEventDefaults {
    private static final double LATITUDE = 90.0;
    private static final double LONGITUDE = 180.0;
    private static final double RADII90 = -1.0;
    private static final Integer FIRST_LAC = 0;
    private static final Integer FIRST_CELL_ID = 0;
    private static final Long DEFAULT_STATUS = 0L;
    private static final int[] DEFAULT_EXT_TAGS = new int[0];
    
    public static void populateDetailMapWithMissingDefaultValues(final Map<String, Object> protocolDetails) {
        addDefaultLatitude(protocolDetails);
        addDefaultLongitude(protocolDetails);
        addDefaultRadii90(protocolDetails);
        addDefaultFirstLac(protocolDetails);
        addDefaultFirstCellId(protocolDetails);
        addDefaultLac(protocolDetails);
        addDefaultCellId(protocolDetails);
        addDefaultStatus(protocolDetails);
        addDefaultExtTags(protocolDetails);
    }
    
    static void addDefaultLatitude(final Map<String, Object> protocolDetails) {
        if (protocolDetails.get("latitude") == null) {
            protocolDetails.put("latitude", LATITUDE);
        }
    }
    
    static void addDefaultLongitude(final Map<String, Object> protocolDetails) {
        if (protocolDetails.get("longitude") == null) {
            protocolDetails.put("longitude", LONGITUDE);
        }
    }
    
    static void addDefaultRadii90(final Map<String, Object> protocolDetails) {
        if (protocolDetails.get("radii90") == null) {
            protocolDetails.put("radii90", RADII90);
        }
    }
    
    static void addDefaultFirstLac(final Map<String, Object> protocolDetails) {
        if (protocolDetails.get("firstLac") == null) {
            protocolDetails.put("firstLac", FIRST_LAC);
        }
    }
    
    static void addDefaultFirstCellId(final Map<String, Object> protocolDetails) {
        if (protocolDetails.get("firstCellId") == null) {
            protocolDetails.put("firstCellId", FIRST_CELL_ID);
        }
    }
    
    static void addDefaultLac(final Map<String, Object> protocolDetails) {
        if (protocolDetails.get("lac") == null) {
            protocolDetails.put("lac", FIRST_LAC);
        }
    }
    
    static void addDefaultCellId(final Map<String, Object> protocolDetails) {
        if (protocolDetails.get("cellId") == null) {
            protocolDetails.put("cellId", FIRST_CELL_ID);
        }
    }
    
    static void addDefaultStatus(final Map<String, Object> protocolDetails) {
        if (protocolDetails.get("status") == null) {
            protocolDetails.put("status", ProtocolEventDefaults.DEFAULT_STATUS);
        }
    }
    
    static void addDefaultExtTags(final Map<String, Object> protocolDetails) {
        if (protocolDetails.get("extTags") == null) {
            protocolDetails.put("extTags", DEFAULT_EXT_TAGS);
        }
    }    
}
