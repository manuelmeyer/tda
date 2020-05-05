package com.dell.rti4t.xd.kafkasource;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

public class CellTower {
    private static final long serialVersionUID = 8974799313012341903L;
    private static final String CVS_HEADER = "#cellid,cellname,centroidEasting,centroidNorthing,sitePostCode,siteLatitude,siteLongitude,siteEasting,siteNorthing,radii90,lac,centroidLatitude,centroidLongitude,cellType,upstreamBandwidth,downstreamBandwidth";
    private int cellID;
    private int lac;
    private String cellName;
    private String sitePostCode;
    private double siteEasting;
    private double siteNorthing;
    private double siteLatitude;
    private double siteLongitude;
    private double centroidEasting;
    private double centroidNorthing;
    private double centroidLatitude;
    private double centroidLongitude;
    private double radii90;
    private double radii80;
    private double radii70;
    private CellType cellType;
    private int upstreamBandwidth;
    private int downstreamBandwidth;
    private final String key;
    private transient String csv;
    private transient String stringed;
    
    public CellTower() {
        this(-1, -1);
    }
    
    public CellTower(final int cellId, final int lac) {
        this(cellId, lac, "UNKNOWN", -1.0, -1.0, "UNKNOWN", 90.0, 180.0, -1.0, -1.0, 90.0, 180.0, CellType.NA, -1.0, -1.0, -1.0);
    }
    
    public CellTower(final int cellID, final int lac, final String cellName, final double centroidEasting, final double centroidNorthing, final String sitePostCode, final double siteLatitude, final double siteLongitude, final double siteEasting, final double siteNorthing, final double centroidLat, final double centroidLng, final CellType type, final double radii90, final double radii80, final double radii70) {
        super();
        this.key = keyFromLacAndId(lac, cellID);
        this.cellID = cellID;
        this.lac = lac;
        this.cellName = cellName;
        this.centroidEasting = centroidEasting;
        this.centroidNorthing = centroidNorthing;
        this.sitePostCode = sitePostCode;
        this.siteLatitude = siteLatitude;
        this.siteLongitude = siteLongitude;
        this.siteEasting = siteEasting;
        this.siteNorthing = siteNorthing;
        this.centroidLatitude = centroidLat;
        this.centroidLongitude = centroidLng;
        this.cellType = type;
        this.radii90 = radii90;
        this.radii80 = radii80;
        this.radii70 = radii70;
    }
    
    public CellTower(final int cellID, final int lac, final String cellName, final double centroidEasting, final double centroidNorthing, final String sitePostCode, final double siteLatitude, final double siteLongitude, final double siteEasting, final double siteNorthing, final double centroidLat, final double centroidLng, final CellType type, final double radii90, final double radii80, final double radii70, final int upstreamBandwidth, final int downstreamBandwidth) {
        this(cellID, lac, cellName, centroidEasting, centroidNorthing, sitePostCode, siteLatitude, siteLongitude, siteEasting, siteNorthing, centroidLat, centroidLng, type, radii90, radii80, radii70);
        this.upstreamBandwidth = upstreamBandwidth;
        this.downstreamBandwidth = downstreamBandwidth;
    }
    
    @JsonProperty
    public String getKey() {
        return this.key;
    }
    
    public static String keyFromLacAndId(final int lac, final int cellId) {
        final StringBuilder s = new StringBuilder();
        s.append(lac);
        s.append('_');
        s.append(cellId);
        return s.toString();
    }
    
    public void setCellID(final int cellID) {
        this.cellID = cellID;
    }
    
    @JsonProperty
    public int getCellID() {
        return this.cellID;
    }
    
    public void setCellLAC(final int lac) {
        this.lac = lac;
    }
    
    @JsonProperty("lac")
    public int getCellLAC() {
        return this.lac;
    }
    
    @JsonProperty
    public String getCellName() {
        return this.cellName;
    }
    
    @JsonProperty
    public double getCentroidEasting() {
        return this.centroidEasting;
    }
    
    @JsonProperty
    public double getCentroidNorthing() {
        return this.centroidNorthing;
    }
    
    @JsonProperty
    public String getSitePostCode() {
        return this.sitePostCode;
    }
    
    @JsonProperty
    public double getSiteLatitude() {
        return this.siteLatitude;
    }
    
    @JsonProperty
    public double getSiteLongitude() {
        return this.siteLongitude;
    }
    
    @JsonProperty
    public double getSiteEasting() {
        return this.siteEasting;
    }
    
    @JsonProperty
    public double getSiteNorthing() {
        return this.siteNorthing;
    }
    
    @JsonProperty
    public double getRadii90() {
        return this.radii90;
    }
    
    @JsonProperty
    public double getRadii80() {
        return this.radii80;
    }
    
    @JsonProperty
    public double getRadii70() {
        return this.radii70;
    }
    
    @JsonProperty
    public double getCentroidLatitude() {
        return this.centroidLatitude;
    }
    
    @JsonProperty
    public double getCentroidLongitude() {
        return this.centroidLongitude;
    }
    
    @JsonProperty
    public int getUpstreamBandwidth() {
        return this.upstreamBandwidth;
    }
    
    @JsonProperty
    public int getDownstreamBandwidth() {
        return this.downstreamBandwidth;
    }
    
    @JsonIgnore
    public int getTotalBandwidth() {
        return this.upstreamBandwidth + this.downstreamBandwidth;
    }
    
    @JsonProperty
    public CellType getCellType() {
        return this.cellType;
    }
    
    @JsonIgnore
    public boolean is2G() {
        return CellType.G2 == this.cellType;
    }
    
    @JsonIgnore
    public boolean is3G() {
        return CellType.G3 == this.cellType;
    }
    
    @JsonIgnore
    public boolean is4G() {
        return CellType.G4 == this.cellType;
    }
    
    @JsonIgnore
    public boolean isSiteLatitudeValid() {
        return this.siteLatitude != 90.0;
    }
    
    @JsonIgnore
    public boolean isSiteLongitudeValid() {
        return this.siteLongitude != 180.0;
    }
    
    @JsonIgnore
    public boolean isSiteLatLongValid() {
        return this.isSiteLatitudeValid() && this.isSiteLongitudeValid();
    }
    
    @JsonIgnore
    public boolean isCentroidLatitudeValid() {
        return this.centroidLatitude != 90.0;
    }
    
    @JsonIgnore
    public boolean isCentroidLongitudeValid() {
        return this.centroidLongitude != 180.0;
    }
    
    @JsonIgnore
    public boolean isCentroidLatLongValid() {
        return this.isCentroidLatitudeValid() && this.isCentroidLongitudeValid();
    }
    
    @JsonIgnore
    public boolean isWGS84CoordinatesValid() {
        return this.isCentroidLatLongValid() && this.isSiteLatLongValid();
    }
    
    @JsonIgnore
    public boolean isRadii90Available() {
        return this.radii90 != -1.0;
    }
    
    @JsonIgnore
    public boolean isRadii80Available() {
        return this.radii80 != -1.0;
    }
    
    @JsonIgnore
    public boolean isRadii70Available() {
        return this.radii70 != -1.0;
    }
    
    public void setSitePostCode(final String sitePostCode) {
        this.sitePostCode = sitePostCode;
    }
    
    @JsonIgnore
    public String getCSVHeader() {
        return "#cellid,cellname,centroidEasting,centroidNorthing,sitePostCode,siteLatitude,siteLongitude,siteEasting,siteNorthing,radii90,lac,centroidLatitude,centroidLongitude,cellType,upstreamBandwidth,downstreamBandwidth";
    }
    
    @JsonIgnore
    public String toCSV() {
        if (this.csv != null) {
            return this.csv;
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(this.cellID);
        builder.append(',');
        builder.append(this.cellName);
        builder.append(',');
        builder.append(this.centroidEasting);
        builder.append(',');
        builder.append(this.centroidNorthing);
        builder.append(',');
        builder.append(this.sitePostCode);
        builder.append(',');
        builder.append(this.siteLatitude);
        builder.append(',');
        builder.append(this.siteLongitude);
        builder.append(',');
        builder.append(this.siteEasting);
        builder.append(',');
        builder.append(this.siteNorthing);
        builder.append(',');
        builder.append(this.radii90);
        builder.append(',');
        builder.append(this.lac);
        builder.append(',');
        builder.append(this.centroidLatitude);
        builder.append(',');
        builder.append(this.centroidLongitude);
        builder.append(',');
        builder.append(this.cellType);
        builder.append(',');
        builder.append(this.radii80);
        builder.append(',');
        builder.append(this.radii90);
        builder.append(',');
        builder.append(this.upstreamBandwidth);
        builder.append(',');
        builder.append(this.downstreamBandwidth);
        return this.csv = builder.toString();
    }
    
    @Override
    public String toString() {
        if (this.stringed != null) {
            return this.stringed;
        }
        final StringBuilder builder = new StringBuilder(super.toString());
        builder.append(" [cellID=");
        builder.append(this.cellID);
        builder.append(", cellName=");
        builder.append(this.cellName);
        builder.append(", centroidEasting=");
        builder.append(this.centroidEasting);
        builder.append(", centroidNorthing=");
        builder.append(this.centroidNorthing);
        builder.append(", sitePostCode=");
        builder.append(this.sitePostCode);
        builder.append(", siteLatitude=");
        builder.append(this.siteLatitude);
        builder.append(", siteLongitude=");
        builder.append(this.siteLongitude);
        builder.append(", siteEasting=");
        builder.append(this.siteEasting);
        builder.append(", siteNorthing=");
        builder.append(this.siteNorthing);
        builder.append(", radii90=");
        builder.append(this.radii90);
        builder.append(", lac=");
        builder.append(this.lac);
        builder.append(", centroidLatitude=");
        builder.append(this.centroidLatitude);
        builder.append(", centroidLongitude=");
        builder.append(this.centroidLongitude);
        builder.append(", cellType=");
        builder.append(this.cellType);
        builder.append(", radii80=");
        builder.append(this.radii80);
        builder.append(", radii70=");
        builder.append(this.radii70);
        builder.append(", upstreamBandwidth=");
        builder.append(this.upstreamBandwidth);
        builder.append(", downstreamBandwidth=");
        builder.append(this.downstreamBandwidth);
        builder.append("]");
        return this.stringed = builder.toString();
    }

}
