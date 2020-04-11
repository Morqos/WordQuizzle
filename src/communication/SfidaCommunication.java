package communication;

import org.json.simple.JSONObject;

public class SfidaCommunication {
	
	private String wordITAToTranslate;
	private String wordInputClient;
	private String nickname;
	private Boolean isTimeoutOver = false;
	
	public SfidaCommunication(){}
	
	public void setWordITAToTranslate(String wordITAToTranslate) { this.wordITAToTranslate = wordITAToTranslate; }
	
	public void setWordInputClient(String wordInputClient) { this.wordInputClient = wordInputClient; }
	
	public void setNickname(String nickname) { this.nickname = nickname; }
	
	public void setTimeoutOver() { this.isTimeoutOver = true; }
	
	public JSONObject getAsJSONObject() {
		JSONObject jsonSfida = new JSONObject();
		if(this.wordITAToTranslate!=null) jsonSfida.put("wordITAToTranslate", this.wordITAToTranslate);
		if(this.wordInputClient!=null) jsonSfida.put("wordInputClient", this.wordInputClient);
		if(this.nickname!=null) jsonSfida.put("nickname", this.nickname);
		jsonSfida.put("isTimeoutOver", this.isTimeoutOver.toString());
		return jsonSfida;
	}

}
