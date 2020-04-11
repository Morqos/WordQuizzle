package main;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import communication.Request;
import communication.Response;
import communication.SfidaCommunication;
import operations.*;
import threadpool.*;

public class MainClassWQClient {
	
	static int portServer = MainClassWQServer.portServer;
	private static InetSocketAddress hostAddress;
	private static SocketChannel socketChannel;
    private static boolean loginDone = false;
    private static String nicknameUser = null;
    //public static Scanner inputScanner = new Scanner(System.in);
    public static boolean sfidaStarted = false;
    public static boolean sfidaJustEnded = false;
    public static boolean waitingSfidaResponse = false;
    public static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        
    
    public static boolean printErrorMessageOutcomeRequest(Response response)
    {
    	if(response == null) return false;
    	if(!response.getOperationSucceded()) {
    		switch(response.getError()) {
				case nickname_gia_presente:
					System.out.println("Errore: nickname gia' presente");
					break;
				case password_vuota:
					System.out.println("Errore: password vuota");
					break;
					
				case login_gia_effettuato:
					System.out.println("Errore: login gia' effettuato");
					break;
				case nickname_o_password_errata:
					System.out.println("Errore: nickname/password incorretti");
					break;
					
				case user_richiesta_amico_inesistente:
					System.out.println("Errore: user richiesta amico inesistente");
					break;
				case amicizia_gia_esistente:
					System.out.println("Errore: amicizia gia' esistente");
					break;
					
				case user_non_amico:
					System.out.println("Errore: user non amico");
					break;
					
				case richiesta_sfida_rifiutata:
					System.out.println("La sfida è stata rifiutata");
					break;
					
				default:
					System.out.println("Errore: non e' possibile effettuare operazione");
	    	}
    	}
    	return false;
    }
     
    
    public static boolean printWrongNumberOfArguments(String operation)
    {
    	System.out.println("numero argomenti errato per operazione " + operation);
		return false;
    }
    
    public static boolean printLoginNotDoneYet(String operation) {
    	System.out.println("effettuare login prima di eseguire operazione " + operation);
		return false;
    }
    
    public static void printRankingFriendsList(Response response)
    {
    	Vector<Response.friendSupportClass> rankingWithFriendsList = new Vector<Response.friendSupportClass>();
    	rankingWithFriendsList = response.getRankingWithFriendsList();
    	System.out.printf("Classifica: ");
    	for(int i=0; i<rankingWithFriendsList.size(); i++)
    	{
    		System.out.printf(rankingWithFriendsList.get(i).getNickname()+ " "+ rankingWithFriendsList.get(i).getScore());
    		if(i!=rankingWithFriendsList.size()-1) System.out.printf(", ");
    	}
    	System.out.printf("%n");
    }
    
    public static void printFriendsList(Response response)
    {
    	Vector<String> friendsList = new Vector<String>();
    	friendsList = response.getFriendsList();
    	for(int i=0; i<friendsList.size(); i++)
    	{
			System.out.printf(friendsList.get(i));
			if(i!=friendsList.size()-1) System.out.printf(", ");
		}

    	System.out.printf("%n");
    }
    
    public static void launchUDPThread()
    {
    	ClientUDPThread threadUDP = new ClientUDPThread(nicknameUser);
        Thread thread = new Thread(threadUDP, "New Thread");
        thread.start();
    }
	
    
    public static void writeBufferInChannel(Request request) throws IOException
    {
		String messageRequest = new String(request.getAsJSONObject().toString());  
		ByteBuffer bufferRequest = ByteBuffer.allocate(MainClassWQServer.nBytesInBuffer);
    	
		Operations.effectiveWriteInChannel(messageRequest,bufferRequest, socketChannel);
	}
    
    public static Response readBufferInChannel() throws IOException
    {
    	Response response = new Response(false, null, null);        
        JSONObject jsonObject = Operations.effectiveReadInChannel(socketChannel);
        
        if(jsonObject==null) return null;
        
        response.fromJSONObjectToResponse(jsonObject);
        return response;
    }
    
    public static void printOutcomeSfida(JSONObject jsonSfidaOutcome) {
    	int nCorrectWords = Integer.parseInt((String)jsonSfidaOutcome.get("nCorrectWords")); 
    	int nWrongWords = Integer.parseInt((String)jsonSfidaOutcome.get("nWrongWords"));
    	int nNonAnsweredWords = Integer.parseInt((String)jsonSfidaOutcome.get("nNonAnsweredWords"));
    	
    	int yourFinalScore = Integer.parseInt((String)jsonSfidaOutcome.get("yourFinalScore"));
    	int opponentFinalScore = Integer.parseInt((String)jsonSfidaOutcome.get("opponentFinalScore"));
    	
    	System.out.println("Hai tradotto correttamente "+nCorrectWords+" parole, ne hai sbagliate "+nWrongWords+" e non hai risposto a "+nNonAnsweredWords+".");
    	System.out.println("Hai totalizzato "+yourFinalScore+" punti.");
    	System.out.println("Il tuo avversario ha totalizzato "+opponentFinalScore+" punti.");
    	if(yourFinalScore>opponentFinalScore)
    		System.out.println("Congratulazioni, hai vinto! Hai guadagnato "+MainClassWQServer.pointsWinnerSfida+" punti extra, per un totale di "+(yourFinalScore+MainClassWQServer.pointsWinnerSfida)+" punti!");
    }
    
    
    //WORK HERE
    public static SfidaCommunication setUpSfidaCommunication(String wordITAToTranslate, String inputWordTranslated)
    {
    	SfidaCommunication sfidaResponse = new SfidaCommunication();
		sfidaResponse.setNickname(nicknameUser);
		sfidaResponse.setWordITAToTranslate(wordITAToTranslate);
		sfidaResponse.setWordInputClient(inputWordTranslated);
    	return sfidaResponse;
    }
    
    public static void prepareSfidaCommunicationWrite(SfidaCommunication sfidaResponse, SocketChannel socketChannelSfida)
    {
		String messageSfidaResponse = new String(sfidaResponse.getAsJSONObject().toString());  
		ByteBuffer bufferSfidaResponse = ByteBuffer.allocate(MainClassWQServer.nBytesInBuffer);
		
		Operations.effectiveWriteInChannel(messageSfidaResponse,bufferSfidaResponse, socketChannelSfida);
    }
    
    public static void sfidaStart(String nicknameSender) throws IOException
    {
    	InetSocketAddress hostAddressSfida = new InetSocketAddress("localhost", Operations.computePort(nicknameSender));
    	
    	try { Thread.sleep(3000); } catch (InterruptedException e1) { e1.printStackTrace(); }
    	
    	SocketChannel socketChannelSfida = null;
    	try { socketChannelSfida = SocketChannel.open(hostAddressSfida); } catch (IOException e) { e.printStackTrace(); }
    	final SocketChannel socketChannelSfidaFinal = socketChannelSfida;
    	
    	System.out.println("Via alla sfida di traduzione!");
    	System.out.println("Avete "+ MainClassWQServer.timeoutSfida/1000 +" secondi per tradurre correttamente "+ MainClassWQServer.KRandomWordsFromDictionary +" parole");
    	
    	try {
			TimeLimitedCodeBlock.runWithTimeout(new Runnable() {
				@Override
			    public void run() {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); String inputWordTranslated;
					   
					for(int i=0; i<MainClassWQServer.KRandomWordsFromDictionary; i++)
			    	{
			    		JSONObject jsonSfidaCommunication = Operations.effectiveReadInChannel(socketChannelSfidaFinal); //Read
			    		String wordITAToTranslate = (String) jsonSfidaCommunication.get("wordITAToTranslate");
			            
			            // Print word and Take Input translation
			    		System.out.println("Challenge " + (i+1) + "/" +MainClassWQServer.KRandomWordsFromDictionary+ ": " + wordITAToTranslate);
			    		do {
				    		try { while (!br.ready()) { Thread.sleep(200); } inputWordTranslated = br.readLine(); }
				    		catch (Exception e) { System.out.println("ConsoleInputReadTask() cancelled"); return; }
					    } while ("".equals(inputWordTranslated));
			    		
			    		SfidaCommunication sfidaResponse = setUpSfidaCommunication(wordITAToTranslate,inputWordTranslated);
			    		prepareSfidaCommunicationWrite(sfidaResponse, socketChannelSfidaFinal);
			    	}
			    }
				
			}, MainClassWQServer.timeoutSfida, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			SfidaCommunication sfidaResponse = new SfidaCommunication();
			sfidaResponse.setNickname(nicknameUser);
			sfidaResponse.setTimeoutOver();
			
			prepareSfidaCommunicationWrite(sfidaResponse, socketChannelSfida);
		}
    	
		JSONObject jsonSfidaOutcome = Operations.effectiveReadInChannel(socketChannelSfidaFinal); //Read
    	printOutcomeSfida(jsonSfidaOutcome);
    	sfidaJustEnded = true;
    	sfidaStarted = false;
    	
    }
    
    
    public static boolean handleRequest(String[] commandAndArgs) throws IOException, ClassNotFoundException
    {
    	
    	Request request = new Request(null, null, null, null);
    	String inputCommand = commandAndArgs[0];

    	switch(inputCommand) {
    		case "registra_utente":
    			if(commandAndArgs.length!=3) return printWrongNumberOfArguments(commandAndArgs[0]);
    			
    			request.setCommand(Request.commandType.registra_utente);
    			request.setNickname(commandAndArgs[1]);
    			request.setPassword(commandAndArgs[2]);

    			writeBufferInChannel(request);
    	        Response response_registra_utente = readBufferInChannel();
    	        
    	        if(!response_registra_utente.getOperationSucceded()) return printErrorMessageOutcomeRequest(response_registra_utente);

    	        // Esito positivo: si può procedere con la registrazione utente
    	        try{
    	        	Registry reg= LocateRegistry.getRegistry(1099);
    	        	Registra_utente stub_registra_utente = (Registra_utente) reg.lookup(Registra_utente.SERVICE_NAME);
    	        	if(stub_registra_utente.registra_utente(request.getNickname(), request.getPassword())) System.out.println("Registrazione eseguita con successo");
    	        	else {
    	        		System.out.println("Registrazione fallita");
    	        		return false;
    	        	}
    	        }catch(Exception e){ e.printStackTrace(); }
    			break;
    			
    		case "login":
    			if(commandAndArgs.length!=3) return printWrongNumberOfArguments(commandAndArgs[0]);
    			
    			request.setCommand(Request.commandType.login);
    			request.setNickname(commandAndArgs[1]);
    			request.setPassword(commandAndArgs[2]);
    			
    			writeBufferInChannel(request);
    	        Response response_login = readBufferInChannel();
    	        
    	        if(!response_login.getOperationSucceded()) return printErrorMessageOutcomeRequest(response_login);
    	        loginDone = true;
    	        nicknameUser = JSONWindow.nicknameToLowerCaseString(request.getNickname());
    	        launchUDPThread(); //Here launch waiting thread for UDPDatagrams
    	            	        
    	        System.out.println("Login eseguito con successo");
    			break;
    		
    		case "logout":
    			if(commandAndArgs.length!=1)  return printWrongNumberOfArguments(commandAndArgs[0]);
    			if(!loginDone) return printLoginNotDoneYet(commandAndArgs[0]);
    			
    			request.setCommand(Request.commandType.logout);
    			request.setNickname(nicknameUser);
    			
    			writeBufferInChannel(request);
    	        Response response_logout = readBufferInChannel();
    	        
    	        if(!response_logout.getOperationSucceded()) return printErrorMessageOutcomeRequest(response_logout);
    	        
    	        loginDone = false;
    	        System.out.println("Logout eseguito con successo");
    	        System.exit(0); 
    			break;
				
    		case "aggiungi_amico":
    			if(commandAndArgs.length!=2) return printWrongNumberOfArguments(commandAndArgs[0]);
    			if(!loginDone) return printLoginNotDoneYet(commandAndArgs[0]);
    			
    			request.setCommand(Request.commandType.aggiungi_amico);
    			request.setNickname(nicknameUser);
    			request.setFriend(commandAndArgs[1]);
    			
    			writeBufferInChannel(request);
    	        Response response_aggiungi_amico = readBufferInChannel();
    	        
    	        if(!response_aggiungi_amico.getOperationSucceded()) return printErrorMessageOutcomeRequest(response_aggiungi_amico);
    	        
    	        System.out.println("Amicizia " +request.getNickname()+"-"+request.getFriend()+ " creata");
    			break;
				
    		case "lista_amici":
    			if(commandAndArgs.length!=1)  return printWrongNumberOfArguments(commandAndArgs[0]);
    			if(!loginDone) return printLoginNotDoneYet(commandAndArgs[0]);
    			
    			request.setNickname(nicknameUser);
    			request.setCommand(Request.commandType.lista_amici);

    			writeBufferInChannel(request);
    	        Response response_lista_amici = readBufferInChannel();
    	        
    	        if(!response_lista_amici.getOperationSucceded()) return printErrorMessageOutcomeRequest(response_lista_amici);
    	        
    	        printFriendsList(response_lista_amici);
    			break;
				
    		case "sfida":
    			if(commandAndArgs.length!=2)  return printWrongNumberOfArguments(commandAndArgs[0]);
    			if(!loginDone) return printLoginNotDoneYet(commandAndArgs[0]);
    			
    			request.setNickname(nicknameUser);
    			request.setCommand(Request.commandType.sfida);
    			request.setFriend(commandAndArgs[1]);
    			
    			writeBufferInChannel(request);
    			
    			System.out.println("Sfida a " +request.getFriend()+ " inviata. In attesa di accettazione");
    			
    			waitingSfidaResponse = true;
    	        Response response_sfida = readBufferInChannel();
    	        waitingSfidaResponse = false;
    	        
    	        if(!response_sfida.getOperationSucceded()) return printErrorMessageOutcomeRequest(response_sfida);
    			
    	        sfidaStarted=true;
    	        sfidaStart(nicknameUser);
    			sfidaStarted=false;
    			
    	        break;
				
    		case "mostra_punteggio":
    			if(commandAndArgs.length!=1) return printWrongNumberOfArguments(commandAndArgs[0]);
    			if(!loginDone) return printLoginNotDoneYet(commandAndArgs[0]);
    			
    			request.setCommand(Request.commandType.mostra_punteggio);
    			request.setNickname(nicknameUser);
    			
    			writeBufferInChannel(request);
    	        Response response_mostra_punteggio = readBufferInChannel();
    	        
    	        if(!response_mostra_punteggio.getOperationSucceded()) return printErrorMessageOutcomeRequest(response_mostra_punteggio);
    	        
    	        System.out.println("Punteggio: " + response_mostra_punteggio.getPunteggio());
    			break;
				
    		case "mostra_classifica":
    			if(commandAndArgs.length!=1)  return printWrongNumberOfArguments(commandAndArgs[0]);
    			if(!loginDone) return printLoginNotDoneYet(commandAndArgs[0]);
			
    			request.setNickname(nicknameUser);
    			request.setCommand(Request.commandType.mostra_classifica);
    			
    			writeBufferInChannel(request);
    	        Response response_mostra_classifica = readBufferInChannel();
    	        
    	        if(!response_mostra_classifica.getOperationSucceded()) return printErrorMessageOutcomeRequest(response_mostra_classifica);
    	        
    	        printRankingFriendsList(response_mostra_classifica);
    			break;
				
			default:
				System.out.println("Errore: comando " + inputCommand + " non presente");
				break;
		}  
    	
    	return true;
    }
    
    
    public static void printCLICommands()
    {
		System.out.println("usage: COMMAND [ ARGS ... ]");
		System.out.println("Commands:");
		System.out.println(" registra_utente <nickUtente > <password > registra l'utente");
		System.out.println(" login <nickUtente > <password > effettua il login");
		System.out.println(" logout effettua il logout");
		System.out.println(" aggiungi_amico <nickAmico> crea relazione di amicizia con nickAmico");
		System.out.println(" lista_amici mostra la lista dei propri amici");
		System.out.println(" sfida <nickAmico > richiesta di una sfida a nickAmico");
		System.out.println(" mostra_punteggio mostra il punteggio dell'utente");
		System.out.println(" mostra_classifica mostra una classifica degli amici dell'utente (incluso l'utente stesso)");
    }
    
    public static void initializeTCPSocketChannel()
    {
    	hostAddress = new InetSocketAddress("localhost", 8080);  
    	try { socketChannel = SocketChannel.open(hostAddress); } catch (IOException e) { e.printStackTrace(); }
    }
    
    public static void waitInput()
    {
    	String input = null;
    	if(!sfidaStarted)
    	{
    		if(sfidaJustEnded)
			{
    			try { input = br.readLine(); } catch (IOException e) { e.printStackTrace(); }
    			sfidaJustEnded = false;
			}
    		else
    		{
		    	do {
		    		try { while (!br.ready()) { Thread.sleep(200); } input = br.readLine(); }
		    		catch (Exception e) { System.out.println("ConsoleInputReadTask() cancelled"); return; }
			    } while ("".equals(input));
    		}
	    	String[] CommandAndArgs = input.split("\\s+");
	    	
	    	try  { handleRequest(CommandAndArgs); } catch (Exception e) { e.printStackTrace(); }
    	}
    }
    
    public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException{
    	
    	printCLICommands();
    	initializeTCPSocketChannel();
    	
    	while(true)
    	{
        	waitInput();
    	}
                
    }
}
