package ppl.wiguana.wiguanadatalogger;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

public class Rilevazione {

	private double gpsLon;
	private double gpsLat;
	private double gpsAlt;
	private String inputSignal;
	private String outputSignal;

	public Rilevazione(double gpsLon, double gpsLat, double gpsAlt, String inputSignal, String outputSignal) {
		this.gpsLon = gpsLon;
		this.gpsLat = gpsLat;
		this.gpsAlt = gpsAlt;
		this.inputSignal = inputSignal;
		this.outputSignal = outputSignal;
	}

	public double getGpsLon() {
		return gpsLon;
	}

	public void setGpsLon(double gpsLon) {
		this.gpsLon = gpsLon;
	}

	public double getGpsLat() {
		return gpsLat;
	}

	public void setGpsLat(double gpsLat) {
		this.gpsLat = gpsLat;
	}

	public double getGpsAlt() {
		return gpsAlt;
	}

	public void setGpsAlt(double gpsAlt) {
		this.gpsAlt = gpsAlt;
	}

	public String getInputSignal() {
		return inputSignal;
	}

	public void setInputSignal(String inputSignal) {
		this.inputSignal = inputSignal;
	}

	public String getOutputSignal() {
		return outputSignal;
	}

	public void setOutputSignal(String outputSignal) {
		this.outputSignal = outputSignal;
	}

	public String toJSON(){

		JSONObject jsonObject= new JSONObject();
		try {
			jsonObject.put("gpsLat", getGpsLat());
			jsonObject.put("gpsLon", getGpsLon());
			jsonObject.put("gpsAlt", getGpsAlt());
			jsonObject.put("inputSignal", getInputSignal());
			jsonObject.put("outputSignal", getOutputSignal());

			return jsonObject.toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "Error to JSONize object";
		}

	}


	public Rilevazione fromJSON(String jsonString){
		Gson gson = new Gson();
		Rilevazione rilevazione = gson.fromJson(jsonString,Rilevazione.class);

			return rilevazione;


	}
}
