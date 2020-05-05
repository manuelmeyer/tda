package com.dell.rti4t.xd.kafkasource;

@SuppressWarnings("unused")
public class Location implements Comparable<Location>{

	private static final byte HAS_ALT = 1;
	private static final byte HAS_BRG = 2;
	private static final byte HAS_SPD = 4;
	private static final byte HAS_ACC = 8;
	private static final byte HAS_LCP = 16;
	private static final float THREE_SIXTY = 360.0f;
	private double latitude;
	private double longitude;
	private long timeStamp;
	private String locationProvider;
	private double altitude;
	private float bearing;
	private float speed;
	private float accuracy;
	private byte hasOptionals;

	public Location() {
		super();
	}

	public Location(final double latitude, final double longitude, final long timeStamp, final String locationProvider, final double altitude, final float bearing, final float speed, final float accuracy) {
		super();
		this.setLatitude(latitude);
		this.setLongitude(longitude);
		this.setTimeStamp(timeStamp);
		this.setLocationProvider(locationProvider);
		this.setAltitude(altitude);
		this.setBearing(bearing);
		this.setSpeed(speed);
		this.setAccuracy(accuracy);
	}

	public long getTimeStamp() {
		return this.timeStamp;
	}

	public void setTimeStamp(final long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public double getLatitude() {
		return this.latitude;
	}

	public void setLatitude(final double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return this.longitude;
	}

	public void setLongitude(final double longitude) {
		this.longitude = longitude;
	}

	public String getLocationProvider() {
		return this.locationProvider;
	}

	public void setLocationProvider(final String locationProvider) {
		this.locationProvider = locationProvider;
		this.setHasLocationProvider(locationProvider != null);
	}

	public double getAltitude() {
		return this.altitude;
	}

	public void setAltitude(final double altitude) {
		this.setHasAltitude(true);
		this.altitude = altitude;
	}

	public float getBearing() {
		return this.bearing;
	}

	public void setBearing(float bearing) {
		while (bearing < 0.0) {
			bearing += 360.0f;
		}
		while (bearing > 360.0f) {
			bearing -= 360.0f;
		}
		this.setHasBearing(true);
		this.bearing = bearing;
	}

	public float getSpeed() {
		return this.speed;
	}

	public void setSpeed(final float speed) {
		this.setHasSpeed(true);
		this.speed = speed;
	}

	public float getAccuracy() {
		return this.accuracy;
	}

	public void setAccuracy(final float accuracy) {
		this.setHasAccuracy(true);
		this.accuracy = accuracy;
	}

	public boolean hasAltitude() {
		return (this.hasOptionals & 0x1) > 0;
	}

	public void setHasAltitude(final boolean hasAltitude) {
		if (hasAltitude) {
			this.hasOptionals |= 0x1;
		} else {
			this.hasOptionals &= 0xFFFFFFFE;
		}
	}

	public boolean hasBearing() {
		return (this.hasOptionals & 0x2) > 0;
	}

	public void setHasBearing(final boolean hasBearing) {
		if (hasBearing) {
			this.hasOptionals |= 0x2;
		} else {
			this.hasOptionals &= 0xFFFFFFFD;
		}
	}

	public boolean hasSpeed() {
		return (this.hasOptionals & 0x4) > 0;
	}

	public void setHasSpeed(final boolean hasSpeed) {
		if (hasSpeed) {
			this.hasOptionals |= 0x4;
		} else {
			this.hasOptionals &= 0xFFFFFFFB;
		}
	}

	public boolean hasAccuracy() {
		return (this.hasOptionals & 0x8) > 0;
	}

	public void setHasAccuracy(final boolean hasAccuracy) {
		if (hasAccuracy) {
			this.hasOptionals |= 0x8;
		} else {
			this.hasOptionals &= 0xFFFFFFF7;
		}
	}

	public boolean hasLocationProvider() {
		return (this.hasOptionals & 0x10) > 0;
	}

	public void setHasLocationProvider(final boolean hasLocationProvider) {
		if (hasLocationProvider) {
			this.hasOptionals |= 0x10;
		} else {
			this.hasOptionals &= 0xFFFFFFEF;
		}
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || this.getClass() != o.getClass()) {
			return false;
		}
		final Location location = (Location) o;
		if (Float.compare(location.accuracy, this.accuracy) != 0) {
			return false;
		}
		if (Double.compare(location.altitude, this.altitude) != 0) {
			return false;
		}
		if (Float.compare(location.bearing, this.bearing) != 0) {
			return false;
		}
		if (this.hasOptionals != location.hasOptionals) {
			return false;
		}
		if (Double.compare(location.latitude, this.latitude) != 0) {
			return false;
		}
		if (Double.compare(location.longitude, this.longitude) != 0) {
			return false;
		}
		if (Float.compare(location.speed, this.speed) != 0) {
			return false;
		}
		if (this.timeStamp != location.timeStamp) {
			return false;
		}
		if (this.locationProvider != null) {
			if (this.locationProvider.equals(location.locationProvider)) {
				return true;
			}
		} else if (location.locationProvider == null) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		long temp = Double.doubleToLongBits(this.latitude);
		int result = (int) (temp ^ temp >>> 32);
		temp = Double.doubleToLongBits(this.longitude);
		result = 31 * result + (int) (temp ^ temp >>> 32);
		result = 31 * result + (int) (this.timeStamp ^ this.timeStamp >>> 32);
		result = 31 * result + ((this.locationProvider != null) ? this.locationProvider.hashCode() : 0);
		temp = Double.doubleToLongBits(this.altitude);
		result = 31 * result + (int) (temp ^ temp >>> 32);
		result = 31 * result + ((this.bearing != 0.0f) ? Float.floatToIntBits(this.bearing) : 0);
		result = 31 * result + ((this.speed != 0.0f) ? Float.floatToIntBits(this.speed) : 0);
		result = 31 * result + ((this.accuracy != 0.0f) ? Float.floatToIntBits(this.accuracy) : 0);
		result = 31 * result + this.hasOptionals;
		return result;
	}

	@Override
	public String toString() {
		return "Location{latitude=" + this.latitude + ", longitude=" + this.longitude + ", timeStamp=" + this.timeStamp + ", locationProvider='" + this.locationProvider + '\'' + ", altitude=" + this.altitude + ", bearing=" + this.bearing + ", speed=" + this.speed + ", accuracy=" + this.accuracy + ", hasOptionals=" + this.hasOptionals + '}';
	}

	public int compareTo(final Location o) {
		if (o == null) {
			return 1;
		}
		return (int) Math.rint(o.accuracy - this.accuracy);
	}
}
