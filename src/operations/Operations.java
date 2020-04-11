package operations;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Random;
import java.util.Vector;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import communication.Request;
import communication.Response;
import communication.Response.errorType;
import main.*;
import threadpool.*;

public class Operations {
	
	public static void effectiveWriteInChannel(String messageResponse,ByteBuffer bufferResponse, SocketChannel clientSocketChannel) {
		bufferResponse.put(messageResponse.getBytes());
		bufferResponse.flip();
		while (bufferResponse.hasRemaining()) {
			try { clientSocketChannel.write(bufferResponse); }
			catch (IOException e) { e.printStackTrace(); }
		}
	}
	
    public static JSONObject effectiveReadInChannel(SocketChannel socketChannel)
    {
    	ByteBuffer responseBuffer = ByteBuffer.allocate(MainClassWQServer.nBytesInBuffer);
    	
        try { socketChannel.read(responseBuffer); }
        catch (IOException e1) { e1.printStackTrace(); }
        
        String responseString = new String(responseBuffer.array()).trim();
        try 
        {
        	JSONObject jsonObject = (JSONObject) new JSONParser().parse(responseString);
        	return jsonObject;
        }
        catch (ParseException e) { e.printStackTrace(); }
        return null;
    }
	
	
	public static JSONObject generateJSONUser(String nickname, String password) {
		JSONObject newUser = new JSONObject();
		newUser.put("score", "0");
		newUser.put("password", password);
		newUser.put("nickname", nickname);
		
		JSONArray friends = new JSONArray();
		 
		newUser.put("friends", friends);
		
		return newUser;
	}
	
	// This method only checks if the operation is doable, DON'T write in database here
	public static Response handle_registra_utente(Request request)
	{
		if(request.getPassword().equals("")) return (new Response(false, errorType.password_vuota, request.getNickname()));

		if(!JSONWindow.nicknameNotExisting(request.getNickname())) return (new Response(false, errorType.nickname_gia_presente, request.getNickname()));
		System.out.println(request.getNickname());
		return (new Response(true, null, request.getNickname()));
	}
	
	public static Response handle_login(Request request, SocketChannel clientSocketChannel)
	{
		if(MainClassWQServer.usersOnline.containsValue(request.getNickname()))
		{
			return (new Response(false, errorType.login_gia_effettuato, request.getNickname()));
		}
		
		if(!JSONWindow.usernameAndPasswordCheck(request.getNickname(), request.getPassword()))
		{
			return (new Response(false, errorType.nickname_o_password_errata, request.getNickname()));
		}
		
		lockAndPutUserOnline(request, clientSocketChannel);

		return (new Response(true, null, request.getNickname()));
	}
	
	public static void lockAndPutUserOnline(Request request, SocketChannel clientSocketChannel)
	{
		if(clientSocketChannel==null) return;
		
		MainClassWQServer.lockUsersOnline.lock();
		MainClassWQServer.usersOnline.put(clientSocketChannel, request.getNickname());
		MainClassWQServer.lockUsersOnline.unlock();
	}
	
	public static void lockAndRemoveUserOnline(SocketChannel clientSocketChannel)
	{
		if(clientSocketChannel==null) return;
		
		MainClassWQServer.lockUsersOnline.lock();
		MainClassWQServer.usersOnline.remove(clientSocketChannel);
		MainClassWQServer.lockUsersOnline.unlock();
	}
	
	public static Response handle_logout(Request request, SocketChannel clientSocketChannel)
	{
		lockAndRemoveUserOnline(clientSocketChannel);
		
		return (new Response(true, null, request.getNickname()));
	}

	
	public static Response handle_aggiungi_amico(Request request)
	{
		if(JSONWindow.nicknameNotExisting(request.getFriend())) return (new Response(false, errorType.user_richiesta_amico_inesistente, request.getNickname()));
		if(JSONWindow.userAlreadyInFriendsListCheck(request.getNickname(), request.getFriend())) return (new Response(false, errorType.amicizia_gia_esistente, request.getNickname()));
		
		JSONWindow.insertNewFriend(request.getNickname(), request.getFriend());
		return (new Response(true, null, request.getNickname()));
	}

	
	public static Response handle_lista_amici(Request request)
	{
		Response response = new Response(true, null, request.getNickname());
		response.setFriendsList();
		return response;
	}
	
	
	// Methods sfida - Start
	public static Vector<String> getKWordsFromDictionary()
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader("dizionario_1000_parole_italiane_comuni.txt"));
			Vector<String> kRandomWords = new Vector();
			Vector<Integer> randomNumbersGenerated = new Vector();
			Random rand = new Random();
			int nThLine;
			
			for(int iWord=0; iWord<MainClassWQServer.KRandomWordsFromDictionary; iWord++)
			{
				do { nThLine = rand.nextInt(1000); } while(randomNumbersGenerated.contains(nThLine));
				randomNumbersGenerated.add(nThLine);
			}
			Collections.sort(randomNumbersGenerated);
			
			int indexFile = -1;
			for(int iWord=0; iWord<MainClassWQServer.KRandomWordsFromDictionary; iWord++)
			{
				int nLine = randomNumbersGenerated.get(iWord);
				for(String line; (line = br.readLine()) != null; ) {
					indexFile++;
					if(nLine==indexFile) {
						kRandomWords.add(line);
						break;
					}
				}
			    // line is not visible here.	
			}
			return kRandomWords;
		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}
	
	public static Vector<String> translateKWords(Vector<String> kRandomWords)
	{
		if(kRandomWords==null) return null;
		Vector<String> kRandomWordsTranslated = new Vector<String>();
		for(int iWord=0; iWord<MainClassWQServer.KRandomWordsFromDictionary; iWord++)
		{
			try {
				URL url = new URL(MainClassWQServer.pathName+MainClassWQServer.tagWord+kRandomWords.get(iWord)+"&"+MainClassWQServer.tagLangPair);
				URLConnection uc=url.openConnection();
				uc.connect();
				
				BufferedReader in=new BufferedReader(new InputStreamReader(uc.getInputStream()));
				String line=null;
				StringBuffer sb=new StringBuffer();
				while((line=in.readLine())!=null){ sb.append(line); }
				
				JSONObject obj = (JSONObject) new JSONParser().parse(sb.toString());
				JSONObject responseDataAPI = (JSONObject) obj.get("responseData");
				String translatedText = (String) responseDataAPI.get("translatedText");
				
				kRandomWordsTranslated.add(iWord, translatedText);
			} catch (Exception e) { e.printStackTrace(); }
		}
		return kRandomWordsTranslated;
	}
	
	// UDPResponse: 1/0
	public static boolean waitForUDPResponseChallenge(int port)
	{
		try {
			MainClassWQServer.datagramSocket.setSoTimeout(MainClassWQServer.timeoutUDPResponse); //waits 30 secs until throws InterruptedException
			byte[] buffer = new byte[100];
			DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
			MainClassWQServer.datagramSocket.receive(receivedPacket);
			String byteToString = new String(receivedPacket.getData(), 0, receivedPacket.getLength(), "US-ASCII");
			if(byteToString.toLowerCase().equals("s")) return true;
		} catch (Exception e) { e.printStackTrace(); }
		return false;
	}
	public static int computePort(String nicknameFriend)
	{
		int hash = 7;
		for (int i=0; i<nicknameFriend.length(); i++) { hash = hash*31 + nicknameFriend.charAt(i); }
		
		return Math.abs(hash%10000+1000);
	}
	public static boolean sendUDPRequest(Request request, int port)
	{
		try {
			DatagramSocket clientSocket = new DatagramSocket();
			byte[] buffer=request.getNickname().getBytes("US-ASCII");
			InetAddress address = InetAddress.getByName("Localhost");
			DatagramPacket mypacket = new DatagramPacket(buffer,buffer.length,address,port);
			clientSocket.send(mypacket);
			clientSocket.close();
			return true;
		} catch (Exception e) { e.printStackTrace(); }
		return false;
	}
	public static Response handle_sfida(Request request)
	{
		if(!JSONWindow.userAlreadyInFriendsListCheck(request.getNickname(), request.getFriend())) return (new Response(false, errorType.user_non_amico, request.getNickname()));
		
		MainClassWQServer.lockUsersInSfida.lock();
		if(MainClassWQServer.usersInSfida.contains(request.getFriend()))
		{
			MainClassWQServer.lockUsersInSfida.unlock();
			return (new Response(false, errorType.richiesta_sfida_rifiutata, request.getNickname()));
		}
		
		MainClassWQServer.addUserInVectorUsersInSfida(request.getNickname());
		MainClassWQServer.addUserInVectorUsersInSfida(request.getFriend());
		MainClassWQServer.lockUsersInSfida.unlock();
		
		int portClient = computePort(JSONWindow.nicknameToLowerCaseString(request.getFriend()));
		
		if(!sendUDPRequest(request, portClient))
		{
			MainClassWQServer.lockAndRemoveUsersInVectorUsersInSfida(request);
			return (new Response(false, errorType.richiesta_sfida_rifiutata, request.getNickname()));
		}
		
		if(!waitForUDPResponseChallenge(MainClassWQServer.portServer))
		{
			MainClassWQServer.lockAndRemoveUsersInVectorUsersInSfida(request);
			return (new Response(false, errorType.richiesta_sfida_rifiutata, request.getNickname()));
		}
		
		return (new Response(true, null, request.getNickname()));
	}
	// Methods sfida - End
	
	public static Response handle_mostra_punteggio(Request request)
	{
		int scoreUser = JSONWindow.getScore(request.getNickname());
		Response response = new Response(true, null, request.getNickname());
		response.setPunteggio(scoreUser);
		return response;
	}

	
	public static Response handle_mostra_cLassifica(Request request)
	{
		Response response = new Response(true, null, request.getNickname());
		response.setRankingWithFriendsList();
		return response;
	}


}
