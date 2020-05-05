package com.dell.rti4t.xd.kafkasource;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum CellType {
    NA("NA"), 
    G2("2G"), 
    G3("3G"), 
    G4("4G"), 
    G3_FEMTO("3GFEMTO"), 
    G4_FEMTO("4GFEMTO");
    
    public static final Map<String, CellType> LUT;
    private final String typeName;
    
    private CellType(final String typeName) {
        this.typeName = typeName;
    }
    
    public String getTypeName() {
        return this.typeName;
    }
    
    @Override
    public String toString() {
        return this.getTypeName();
    }
    
    static {
        final Map<String, CellType> type = new ConcurrentHashMap<String, CellType>(values().length);
        for (final CellType cType : values()) {
            type.put(cType.getTypeName(), cType);
        }
        LUT = Collections.unmodifiableMap((Map<? extends String, ? extends CellType>)type);
    }

}
