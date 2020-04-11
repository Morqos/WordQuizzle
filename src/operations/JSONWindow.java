package operations;
import java.io.*;
import java.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JSONWindow {
	
	static private boolean fileInstantiated = false;
	static private boolean firstWriteDone = false;
	static private File file;
	
	public JSONWindow() {
		if (fileInstantiated==false) {
			try {
				file = new File("database.json");
				
				if(!file.exists()) file.createNewFile();
				else firstWriteDone = true;
				
				fileInstantiated=true;
			} catch (IOException e) {
				 e.printStackTrace();
			}
		}
	}
	
	
	public static synchronized boolean effectiveWrite(JSONObject jsonObject, String whatToPrint) {
		try {
			FileWriter fileWriter = new FileWriter(file);
			System.out.println(whatToPrint);
			System.out.println("-----------------------");
			fileWriter.write(jsonObject.toJSONString());
			fileWriter.flush();
			fileWriter.close();
		}catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	
	public static JSONArray readUsersJSON() {
		
		JSONParser parser = new JSONParser();
		JSONArray arrayUsers = new JSONArray();
		try {
			if(firstWriteDone) {
				Object obj = parser.parse(new FileReader(file));
				JSONObject jsonObject = (JSONObject) obj; 
				
				arrayUsers = (JSONArray) jsonObject.get("users");
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); } 
		catch (IOException e) { e.printStackTrace(); }
		catch (ParseException e) { e.printStackTrace(); }
		
		return arrayUsers;
	
	}
	
	public static JSONArray readFriendsUserJSON(String userNickname) {
		String newUserNicknameLowerCase = nicknameToLowerCaseString(userNickname);
		JSONArray arrayUsers = readUsersJSON();
		
		Iterator<JSONObject> iteratorJSONArray = arrayUsers.iterator();
		while (iteratorJSONArray.hasNext()) {
			JSONObject jsonUser = iteratorJSONArray.next();
			String nickname = (String) jsonUser.get("nickname");
		
			if(nickname.equals(newUserNicknameLowerCase)) {
				return (JSONArray) jsonUser.get("friends");
			}
		}
		
		return null;
	}
		

	public static boolean firstWriteJSON(JSONObject newUser){
		JSONArray arrayUsers = new JSONArray();
		arrayUsers.add(newUser);

		JSONObject mainObj = new JSONObject();
		mainObj.put("users", arrayUsers);
		
		boolean effectiveWriteReturn = effectiveWrite(mainObj, "Writing JSON users to file: first time");
		if(effectiveWriteReturn == false) return false;
		
		firstWriteDone = true;
		return true;
	}
	
	
	// true if nickname is not in list users
	public static boolean nicknameNotExisting(String newUserNickname) {
		String newUserNicknameLowerCase = nicknameToLowerCaseString(newUserNickname);
		JSONArray arrayUsers = readUsersJSON();
		
		Iterator<JSONObject> iteratorJSONArray = arrayUsers.iterator();
		while (iteratorJSONArray.hasNext()) {
			JSONObject jsonUser = iteratorJSONArray.next();
			String nickname = (String) jsonUser.get("nickname");
		
			if(nickname.equals(newUserNicknameLowerCase)) {
				System.out.println("nickname already existing");
				return false;
			}
		}
		
		return true;
	}
	
	// true if nickname and password are both in list users
	public static boolean usernameAndPasswordCheck(String userNickname, String userPassword) {
		String newUserNicknameLowerCase = nicknameToLowerCaseString(userNickname);
		JSONArray arrayUsers = readUsersJSON();
		
		Iterator<JSONObject> iteratorJSONArray = arrayUsers.iterator();
		while (iteratorJSONArray.hasNext()) {
			JSONObject jsonUser = iteratorJSONArray.next();
			String nickname = (String) jsonUser.get("nickname");
		
			if(nickname.equals(newUserNicknameLowerCase)) {
				String password = (String) jsonUser.get("password");
			
				if(password.equals(userPassword)) {
					System.out.println("nickname correct, password correct");
					return true;
				}
				
				System.out.println("nickname correct, password wrong");
				return false;
			}
		}
		
		System.out.println("nickname not found");
		return false;
	}

	
	// true if user is in list Friend
	public static boolean userAlreadyInFriendsListCheck(String userNickname, String nicknameFriendToAdd) {
		String newUserNicknameLowerCase = nicknameToLowerCaseString(userNickname);
		JSONArray arrayFriendsUser = readFriendsUserJSON(newUserNicknameLowerCase);
		
		Iterator<String> iteratorJSONArray = arrayFriendsUser.iterator();
		while (iteratorJSONArray.hasNext()) {
			String nicknameTmpFriend = iteratorJSONArray.next();
			//String nicknameTmpFriend = (String) jsonUser.get("nickname");
			
			if(nicknameFriendToAdd.equals(nicknameTmpFriend)) {
				return true;
			}
		}
		
		return false;
	}
	
	
	public static boolean nextWritesJSON(JSONObject newUser){
		// is nickname not already present in database?
		if(!nicknameNotExisting((String) newUser.get("nickname"))) return false;

		JSONArray arrayUsers = readUsersJSON();
		arrayUsers.add(newUser);
		
		JSONObject mainObj = new JSONObject();
		mainObj.put("users", arrayUsers);
		
		return effectiveWrite(mainObj, "Adding JSON user in file");
	}		
	
	
	public static String nicknameToLowerCaseString(String newUserNickname) {
		if(newUserNickname==null) return null;
		String nicknameLowerCase = newUserNickname.toLowerCase();
		return nicknameLowerCase;
	}
	
	
	public static boolean nicknameToLowerCaseJSON(JSONObject newUser) {
		if(newUser==null) return false;
		String nickname = (String) newUser.get("nickname");
		String nicknameLowerCase = nicknameToLowerCaseString(nickname);
		newUser.put("nickname", nicknameLowerCase);
		return true;
	}
	
	public static boolean insertNewUser(JSONObject newUser){
		nicknameToLowerCaseJSON(newUser);
		
		if(firstWriteDone == false) return firstWriteJSON(newUser);
		else return nextWritesJSON(newUser);
	}

	public static boolean insertNewFriend(String userNickname, String nicknameFriendToAdd) {
		
		String userNicknameLowerCase = nicknameToLowerCaseString(userNickname);
		String nicknameFriendToAddLowerCase = nicknameToLowerCaseString(nicknameFriendToAdd);
		
		JSONArray arrayFriendsUser = readFriendsUserJSON(userNicknameLowerCase);
		arrayFriendsUser.add(nicknameFriendToAddLowerCase);
		
		
		JSONArray arrayFriendsFriend = readFriendsUserJSON(nicknameFriendToAddLowerCase);
		arrayFriendsFriend.add(userNicknameLowerCase);
		
		JSONArray arrayUsers = readUsersJSON();
		Iterator<JSONObject> iteratorJSONArray = arrayUsers.iterator();
		while (iteratorJSONArray.hasNext()) {
			JSONObject jsonUserTmp = iteratorJSONArray.next();
			String nicknameTmp = (String) jsonUserTmp.get("nickname");
			
			if(userNicknameLowerCase.equals(nicknameTmp)) {
				jsonUserTmp.remove("friends");
				jsonUserTmp.put("friends", arrayFriendsUser);
			}
			if(nicknameFriendToAddLowerCase.equals(nicknameTmp)) {
				jsonUserTmp.remove("friends");
				jsonUserTmp.put("friends", arrayFriendsFriend);
			}
		}
		
		JSONObject mainObj = new JSONObject();
		mainObj.put("users", arrayUsers);
		
		return effectiveWrite(mainObj, "Updating JSON user in file: created new friendship");
	}
	
	public static boolean updateScore(String nickname, int scoreToAdd) {
		if(nickname==null) return false;
		
		String nicknameLowerCase = nicknameToLowerCaseString(nickname);
		JSONArray arrayUsers = readUsersJSON();
		
		Iterator<JSONObject> iteratorJSONArray = arrayUsers.iterator();
		while (iteratorJSONArray.hasNext()) {
			JSONObject jsonUserTmp = iteratorJSONArray.next();
			String nicknameTmp = (String) jsonUserTmp.get("nickname");
		
			if(nicknameTmp.equals(nicknameLowerCase)) {
				int finalScore = Integer.parseInt((String) jsonUserTmp.get("score"))+scoreToAdd;
				jsonUserTmp.remove("score");
				jsonUserTmp.put("score", Integer.toString(finalScore));
				break;
			}
		}
		JSONObject mainObj = new JSONObject();
		mainObj.put("users", arrayUsers);
		
		return effectiveWrite(mainObj, "Updating JSON user in file: updated score");
	}
	
	public static int getScore(String nickname) {
		
		String newUserNicknameLowerCase = nicknameToLowerCaseString(nickname);
		JSONArray arrayUsers = readUsersJSON();
		
		Iterator<JSONObject> iteratorJSONArray = arrayUsers.iterator();
		while (iteratorJSONArray.hasNext()) {
			JSONObject jsonUserTmp = iteratorJSONArray.next();
			String nicknameTmp = (String) jsonUserTmp.get("nickname");
		
			if(nickname.equals(nicknameTmp)) {
				return Integer.parseInt((String) jsonUserTmp.get("score"));
			}
		}
		return -1;
	}
}
