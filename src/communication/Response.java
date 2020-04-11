package communication;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import communication.Request.commandType;
import operations.*;

public class Response{
	
	public enum errorType {
		nickname_gia_presente, //registra_utente
		password_vuota,
		
		login_gia_effettuato, //login
		nickname_o_password_errata,
		
		user_richiesta_amico_inesistente, //aggiungi_amico
		amicizia_gia_esistente,
		
		user_non_amico, //sfida
		richiesta_sfida_rifiutata,
		
		non_disponibile
	}
	
	public class friendSupportClass{
		private int score;
		private String nickname;
		
		public friendSupportClass(int score, String nickname)
		{
			this.score=score;
			this.nickname=nickname;
		}
		
		public void setScore(int score) { this.score = score; }
		public void setNickname(String nickname) { this.nickname = nickname; }
		
		public int getScore() { return this.score; }
		public String getNickname() { return this.nickname; }
	}
	
	class SortByScore implements Comparator<friendSupportClass> 
	{ 
	    // Used for sorting in descending order of score
	    public int compare(friendSupportClass friendA, friendSupportClass friendB) 
	    { 
	        return friendB.getScore() - friendA.getScore(); 
	    } 
	} 
	
	private String nickname;
	private Boolean operationSucceded;
	private errorType error;
	private int punteggio = 0;
	private Vector<String> friendsList;
	private Vector<friendSupportClass> rankingWithFriendsList;
	
	
	public Response(boolean operationSucceded, errorType error, String nickname) {
		friendsList = new Vector<String>();
		rankingWithFriendsList = new Vector<friendSupportClass>();
		this.nickname = nickname;
		this.operationSucceded = operationSucceded;
		if(!operationSucceded && error==null) { this.error = errorType.non_disponibile; return; }
		this.error = error;
	}
	
	
	public boolean getOperationSucceded() { return this.operationSucceded; }
	public errorType getError() { return this.error; }
	
	public void setNickname(String nickname) { this.nickname=nickname; }
	public void setPunteggio(int punteggio) { this.punteggio=punteggio; }
	public int getPunteggio() { return this.punteggio; }
	public String getNickname() { return this.nickname; }
	
	public JSONObject getAsJSONObject() {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("operationSucceded", this.operationSucceded.toString());
		jsonResponse.put("nickname", this.nickname);
		if(this.error!=null) jsonResponse.put("error", this.error.name());
		if(this.error==null) jsonResponse.put("error", "non_disponibile");
		jsonResponse.put("punteggio", Integer.toString(this.punteggio));
		jsonResponse.put("friendsList", getFriendsListAsJSONArray(this.nickname));
		jsonResponse.put("rankingWithFriendsList",getRankingWithFriendsListAsJSONArray());
		return jsonResponse;
	}
	
	public void fromJSONObjectToResponse(JSONObject jsonObject) {
		this.operationSucceded = Boolean.parseBoolean((String) jsonObject.get("operationSucceded"));
		this.nickname = (String) jsonObject.get("nickname");
		this.error = error.valueOf((String) jsonObject.get("error"));
		this.punteggio = Integer.parseInt((String)jsonObject.get("punteggio"));
		this.friendsList = friendsListFromJSONtoVectorString((JSONArray) jsonObject.get("friendsList"));
		this.rankingWithFriendsList = rankingWithFriendsListFromJSONtoVector((JSONArray) jsonObject.get("rankingWithFriendsList"));
	}

	
	public void fillFriendsList(Vector<String> friendsList, JSONArray arrayUsers)
	{
		if(arrayUsers ==  null) return;
		Iterator<String> iteratorJSONArray = arrayUsers.iterator();
		while (iteratorJSONArray.hasNext()) {
			String nicknameFriend = iteratorJSONArray.next();
			friendsList.add(nicknameFriend);
		}
	}
	public Vector<String> friendsListFromJSONtoVectorString(JSONArray arrayUsers)
	{
		Vector<String> friendsList = new Vector<String>();
		fillFriendsList(friendsList, arrayUsers);
		return friendsList;
	}
	public void setFriendsList()
	{
		this.friendsList.clear();
		fillFriendsList(this.friendsList, JSONWindow.readFriendsUserJSON(this.nickname));
	}
	
	
	
	public Vector<friendSupportClass> rankingWithFriendsListFromJSONtoVector(JSONArray arrayUsers)
	{
		Vector<friendSupportClass> rankingFriendsList = new Vector<friendSupportClass>();
		Iterator<JSONObject> iteratorJSONArray = arrayUsers.iterator();
		while (iteratorJSONArray.hasNext()) {
			JSONObject friendNicknameAndScore = iteratorJSONArray.next();
			rankingFriendsList.add(new friendSupportClass(Integer.parseInt((String)friendNicknameAndScore.get("score")), (String)friendNicknameAndScore.get("nickname")));
		}
		
		Collections.sort(this.rankingWithFriendsList, new SortByScore());
		return rankingFriendsList;
	}
	public void setRankingWithFriendsList()
	{
		this.rankingWithFriendsList.clear();
		setFriendsList();
		Vector<String> tmpFriendsListVector = this.friendsList;
		
		for(String friendNickname: tmpFriendsListVector)
			this.rankingWithFriendsList.add(new friendSupportClass(JSONWindow.getScore(friendNickname), friendNickname));
		this.rankingWithFriendsList.add(new friendSupportClass(JSONWindow.getScore(this.nickname), this.nickname));
		
		Collections.sort(this.rankingWithFriendsList, new SortByScore()); 
	}
	
	
	public Vector<String> getFriendsList() { return this.friendsList; }
	public Vector<friendSupportClass> getRankingWithFriendsList() { return this.rankingWithFriendsList; }
	
	public JSONArray getFriendsListAsJSONArray(String nickname)
	{
		if(nickname==null) throw new NullPointerException();
		return JSONWindow.readFriendsUserJSON(nickname);
	}
	
	
	public JSONArray getRankingWithFriendsListAsJSONArray()
	{
		setRankingWithFriendsList();
		
		JSONArray jsonRankingWithFriendsList = new JSONArray();
		 
		for(friendSupportClass friendInRanking: this.rankingWithFriendsList)
		{
			JSONObject jsonFriendInRanking = new JSONObject();
			jsonFriendInRanking.put("nickname", friendInRanking.getNickname());
			jsonFriendInRanking.put("score", Integer.toString(friendInRanking.getScore()));
			jsonRankingWithFriendsList.add(jsonFriendInRanking);
		}
		
		return jsonRankingWithFriendsList;
	}
}
