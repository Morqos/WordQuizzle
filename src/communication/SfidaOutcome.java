package communication;

import org.json.simple.JSONObject;

public class SfidaOutcome {
	
	private int nCorrectWords;
	private int nWrongWords;
	private int nNonAnsweredWords;
	
	private int yourFinalScore;
	private int opponentFinalScore;
	
	public SfidaOutcome() {}
	
	public void setNCorrectWords(int nCorrectWords) { this.nCorrectWords=nCorrectWords; }
	public void setNWrongWords(int nWrongWords) { this.nWrongWords=nWrongWords; }
	public void setNNonAnsweredWords(int nNonAnsweredWords) { this.nNonAnsweredWords=nNonAnsweredWords; }
	public void setYourFinalScore(int yourFinalScore) { this.yourFinalScore=yourFinalScore; }
	public void setOpponentFinalScore(int opponentFinalScore) { this.opponentFinalScore=opponentFinalScore; }
	
	public JSONObject getAsJSONObject() {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("nCorrectWords", Integer.toString(this.nCorrectWords));
		jsonResponse.put("nWrongWords", Integer.toString(this.nWrongWords));
		jsonResponse.put("nNonAnsweredWords", Integer.toString(this.nNonAnsweredWords));
		jsonResponse.put("yourFinalScore", Integer.toString(this.yourFinalScore));
		jsonResponse.put("opponentFinalScore", Integer.toString(this.opponentFinalScore));
		return jsonResponse;
	}
	
}
