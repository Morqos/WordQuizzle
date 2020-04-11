package communication;
import java.io.Serializable;

import org.json.simple.JSONObject;

public class Request{
	
	public enum commandType {
		registra_utente,
		login,
		logout,
		aggiungi_amico,
		lista_amici,
		sfida,
		mostra_punteggio,
		mostra_classifica
	}
	
	private commandType command;
	private String nickname = null;
	private String password = null;
	private String friend = null;
	
	
	public Request(commandType command, String nickname, String password, String friend)
	{
		this.command = command;
		this.nickname = nickname;
		this.password = password;
		this.password = friend;
	}

	
	public void setCommand(commandType command) {
		this.command = command;
	}
	
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setFriend(String friend) {
		this.friend = friend;
	}
	
	
	
	public commandType getCommand() {
		return this.command;
	}
	
	public String getNickname() {
		return this.nickname;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	public String getFriend() {
		return this.friend;
	}
	
	public JSONObject getAsJSONObject() {
		JSONObject jsonRequest = new JSONObject();
		jsonRequest.put("command", this.command.name());
		jsonRequest.put("nickname", this.nickname);
		jsonRequest.put("password", this.password);
		jsonRequest.put("friend", this.friend);
		return jsonRequest;
	}
	
	public void fromJSONObjectToRequest(JSONObject jsonObject) {
		
		this.command = commandType.valueOf((String) jsonObject.get("command"));
		this.nickname = (String) jsonObject.get("nickname");
		this.password = (String) jsonObject.get("password");
		this.friend = (String) jsonObject.get("friend");
	}
}
