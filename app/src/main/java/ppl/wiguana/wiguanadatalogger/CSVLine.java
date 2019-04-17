package ppl.wiguana.wiguanadatalogger;

public class CSVLine {

	private long millisecondDate;
	private String line;
	private double latitude;
	private double longitude;
	private double altitude;
	private double accuracy;

	public CSVLine(long millisecondDate, String line, double latitude, double longitude, double altitude, double accuracy) {
		this.millisecondDate = millisecondDate;
		this.line = line;
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.accuracy = accuracy;
	}

	public long getMillisecondDate() {
		return millisecondDate;
	}

	public void setMillisecondDate(long millisecondDate) {
		this.millisecondDate = millisecondDate;
	}

	public String getLine() {
		return line;
	}

	public void setLine(String line) {
		this.line = line;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	public double getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
	}

	public String toString(){
		return "Date: " + this.millisecondDate + ", " + this.line + ", Latitude: "+ this.latitude +
			", Longitude: " + this.longitude + ", Altitude: " + this.altitude + ", GPSAccuracy" + this.accuracy +"\n";
	}
}
