package threadpool;

import operations.*;
import main.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import communication.Request;
import communication.Response;
import communication.SfidaCommunication;
import communication.SfidaOutcome;

public class WorkerThread implements Runnable {
	private Request request;
	private SocketChannel clientSocketChannel;

	
	public WorkerThread(Request request, SocketChannel clientSocketChannel) throws IOException{
        this.request = request;
        this.clientSocketChannel = clientSocketChannel;        
    }

	public void writeBufferInChannel(Response response) throws IOException {
		if(response==null) throw new NullPointerException();
		
		String messageResponse = new String(response.getAsJSONObject().toString());  
		ByteBuffer bufferResponse = ByteBuffer.allocate(MainClassWQServer.nBytesInBuffer);
    	
		Operations.effectiveWriteInChannel(messageResponse,bufferResponse,this.clientSocketChannel);
    }
	
    public static void setUpSfidaCommunicationAndWrite(String wordITAToTranslate, SocketChannel clientSocketChannel)
    {
    	SfidaCommunication sfidaResponse = new SfidaCommunication();
		sfidaResponse.setWordITAToTranslate(wordITAToTranslate);
		String messageSfidaResponse = new String(sfidaResponse.getAsJSONObject().toString());  
		ByteBuffer bufferSfidaResponse = ByteBuffer.allocate(MainClassWQServer.nBytesInBuffer);
		Operations.effectiveWriteInChannel(messageSfidaResponse,bufferSfidaResponse,clientSocketChannel);	
    }
    
    public static SfidaOutcome setUpSfidaOutcome(int nCorrectWords, int nWrongWords, int finalScoreHim, int finalScoreOpponent)
    {
    	SfidaOutcome sfidaOutcome = new SfidaOutcome();
		sfidaOutcome.setNCorrectWords(nCorrectWords);
		sfidaOutcome.setNWrongWords(nWrongWords);
		sfidaOutcome.setNNonAnsweredWords(MainClassWQServer.KRandomWordsFromDictionary-(nCorrectWords+nWrongWords));
		sfidaOutcome.setYourFinalScore(finalScoreHim);
		sfidaOutcome.setOpponentFinalScore(finalScoreOpponent);
		return sfidaOutcome;
    }
    
    public static void setUpSfidaOutcomeAndWrite(int nCorrectWords, int nWrongWords, int finalScoreHim, int finalScoreOpponent, SocketChannel clientSocketChannel)
    {
    	SfidaOutcome sfidaOutcome = setUpSfidaOutcome(nCorrectWords,nWrongWords, finalScoreHim,finalScoreOpponent);
		String messageSfidaOutcome = new String(sfidaOutcome.getAsJSONObject().toString());  
		ByteBuffer bufferSfidaOutcome = ByteBuffer.allocate(MainClassWQServer.nBytesInBuffer);
		Operations.effectiveWriteInChannel(messageSfidaOutcome,bufferSfidaOutcome,clientSocketChannel);
    }

	
	public void runSfida(Selector selectorSfida, ServerSocketChannel serverSocketChannel, Vector<String> kRandomWords, Vector<String> kRandomWordsTranslated) throws IOException
	{
		boolean sfidaCompleted = false; boolean firstUserFinished = false;
		boolean senderTimeout = false; boolean receiverTimeout = false;
		int indexWordSender = 0; int indexWordReceiver = 0; int indexWord = 0;
		int countFirstWrite = 0;
		int nCorrectWordsSender = 0; int nWrongWordsSender = 0;
		int nCorrectWordsReceiver = 0; int nWrongWordsReceiver = 0;
		SocketChannel firstWriteSocketChannel = null;
		SocketChannel socketChannelSender = null; SocketChannel socketChannelReceiver = null;
		
		while(!sfidaCompleted){
        	try { int numberOfKeys = selectorSfida.select(); } catch (IOException e1) { e1.printStackTrace(); }

        	Set selectedKeys = selectorSfida.selectedKeys();  
            Iterator iteratorKeys = selectedKeys.iterator();  
            
            while (iteratorKeys.hasNext()) {  
                SelectionKey key = (SelectionKey) iteratorKeys.next();  
                if (key.isAcceptable()) { 
                    SocketChannel client = serverSocketChannel.accept(); // The new client connection is accepted  
                    client.configureBlocking(false);  // The new connection is added to a selector  
                    client.register(selectorSfida, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                }
                else if (key.isReadable()) {
                    SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                    System.out.println("I'm inside runSfida: there's a readable key");

                    JSONObject jsonSfidaCommunication = Operations.effectiveReadInChannel(clientSocketChannel);
                    
                    if(jsonSfidaCommunication.get("isTimeoutOver").equals("true")) // check if isTimeoutOver
                	{
                    	// Set up outcome and write it
                    	int finalScoreSender = (nCorrectWordsSender*MainClassWQServer.pointsCorrectTranslation)+(nWrongWordsSender*MainClassWQServer.pointsWrongTranslation);
                    	int finalScoreReceiver = (nCorrectWordsReceiver*MainClassWQServer.pointsCorrectTranslation)+(nWrongWordsReceiver*MainClassWQServer.pointsWrongTranslation);
                    	if(this.request.getNickname().equals(jsonSfidaCommunication.get("nickname")))
                    	{
                    		senderTimeout=true;
                    		setUpSfidaOutcomeAndWrite(nCorrectWordsSender,nWrongWordsSender, finalScoreSender,finalScoreReceiver, clientSocketChannel);
                    		if(firstUserFinished) sfidaCompleted=true;
                    		else firstUserFinished=true;
                    		int finalScoreToAdd =  finalScoreSender > finalScoreReceiver ? finalScoreSender+MainClassWQServer.pointsWinnerSfida: finalScoreSender;
                    		JSONWindow.updateScore((String)jsonSfidaCommunication.get("nickname"),finalScoreToAdd);
                    	}
                    	else
                    	{
                    		receiverTimeout=true;
                    		setUpSfidaOutcomeAndWrite(nCorrectWordsReceiver,nWrongWordsReceiver, finalScoreReceiver,finalScoreSender, clientSocketChannel);
	                    	if(firstUserFinished) sfidaCompleted=true;
	                		else firstUserFinished=true;
	                    	int finalScoreToAdd =  finalScoreReceiver > finalScoreSender ? finalScoreReceiver+MainClassWQServer.pointsWinnerSfida: finalScoreReceiver;
                    		JSONWindow.updateScore((String)jsonSfidaCommunication.get("nickname"),finalScoreToAdd);
                    	}
                    	continue;
                	}
                    
                    // Update the sfida's score of the user
                    if(this.request.getNickname().equals(jsonSfidaCommunication.get("nickname")))
                    {
                    	// Save SocketChannelSender and check if it is the last word
                    	socketChannelSender=clientSocketChannel;
                    	if(kRandomWordsTranslated.get(indexWordSender).equals((String)jsonSfidaCommunication.get("wordInputClient"))) nCorrectWordsSender++;
                    	else nWrongWordsSender++;
                    	
                    	indexWordSender++; indexWord=indexWordSender;
                    	if(indexWord==kRandomWords.size()-1)
                    	{
                    		if(firstUserFinished) sfidaCompleted=true;
                    		else firstUserFinished=true;
                    	}
                    	else if(indexWord==kRandomWords.size()) continue;
                    }
                    else
                    {
                    	// Save SocketChannelReceiver and check if it is the last word
                    	socketChannelReceiver=clientSocketChannel;
                    	if(kRandomWordsTranslated.get(indexWordReceiver).equals((String)jsonSfidaCommunication.get("wordInputClient"))) nCorrectWordsReceiver++;
                    	else nWrongWordsReceiver++;
                    	
                    	indexWordReceiver++; indexWord=indexWordReceiver;
                    	if(indexWord==kRandomWords.size()-1)
                    	{
                    		if(firstUserFinished) sfidaCompleted=true;
                    		else firstUserFinished=true;
                    	}
                    	else if(indexWord==kRandomWords.size()) continue;
                    }
                    
                    String wordITAToTranslate = kRandomWords.get(indexWord);
                    setUpSfidaCommunicationAndWrite(wordITAToTranslate, clientSocketChannel);
                }
                else if (key.isWritable() && countFirstWrite<2) {
                    SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                    if(!clientSocketChannel.equals(firstWriteSocketChannel))
                    {
	                    String wordITAToTranslate = kRandomWords.get(0);
	                    setUpSfidaCommunicationAndWrite(wordITAToTranslate, clientSocketChannel);
	                    
	            		firstWriteSocketChannel=clientSocketChannel; countFirstWrite++;
                    }
                }
                iteratorKeys.remove();
            }
        }
		
		int finalScoreSender = (nCorrectWordsSender*MainClassWQServer.pointsCorrectTranslation)+(nWrongWordsSender*MainClassWQServer.pointsWrongTranslation);
		int finalScoreReceiver = (nCorrectWordsReceiver*MainClassWQServer.pointsCorrectTranslation)+(nWrongWordsReceiver*MainClassWQServer.pointsWrongTranslation);
    	
		if(!senderTimeout)
		{
			setUpSfidaOutcomeAndWrite(nCorrectWordsSender,nWrongWordsSender, finalScoreSender,finalScoreReceiver, socketChannelSender);
			JSONWindow.updateScore(this.request.getNickname(),finalScoreSender);				
		}
		if(!receiverTimeout)
		{
			setUpSfidaOutcomeAndWrite(nCorrectWordsReceiver,nWrongWordsReceiver, finalScoreReceiver,finalScoreSender, socketChannelReceiver);
			JSONWindow.updateScore(this.request.getFriend(),finalScoreReceiver);
		}
				
	}
	
	public ServerSocketChannel openServerSocketChannel()
	{
		try {
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			InetSocketAddress hostAddress = new InetSocketAddress("localhost", Operations.computePort(this.request.getNickname()));

			serverSocketChannel.bind(hostAddress);  
	        serverSocketChannel.configureBlocking(false);
	        
			return serverSocketChannel;
		} catch (IOException e) { e.printStackTrace(); }
		
		return null;
	}
    
	public Selector openNewTCPSfidaWithSelector(ServerSocketChannel serverSocketChannel)
	{
		try {
			Selector selectorSfida = Selector.open();
	        int validOps = serverSocketChannel.validOps();  
	        SelectionKey selectKey = serverSocketChannel.register(selectorSfida, validOps, null);

	        return selectorSfida;
		} catch (IOException e) { e.printStackTrace(); }
		
		return null;
	}
    
	@Override
	public void run() {
		switch(this.request.getCommand()) {
			case registra_utente:
				Response response_registra_utente = Operations.handle_registra_utente(this.request);
				try { writeBufferInChannel(response_registra_utente); } catch (IOException e) { e.printStackTrace(); }
				break;
				
			case login:
				Response response_login = Operations.handle_login(this.request, this.clientSocketChannel);
				try { writeBufferInChannel(response_login); } catch (IOException e) { e.printStackTrace(); }
				break;
				
			case logout:
				Response response_logout = Operations.handle_logout(this.request, this.clientSocketChannel);
				try { writeBufferInChannel(response_logout); } catch (IOException e) { e.printStackTrace(); }
				break;
				
			case aggiungi_amico:
				Response response_aggiungi_amico = Operations.handle_aggiungi_amico(this.request);
				try { writeBufferInChannel(response_aggiungi_amico); } catch (IOException e) { e.printStackTrace(); }
				break;
				
			case lista_amici:
				Response response_lista_amici = Operations.handle_lista_amici(this.request);
				try { writeBufferInChannel(response_lista_amici); } catch (IOException e) { e.printStackTrace(); }
				break;
				
			case sfida:
				Response response_sfida = Operations.handle_sfida(this.request);
				try { writeBufferInChannel(response_sfida); } catch (IOException e) { e.printStackTrace(); }

				if(response_sfida.getOperationSucceded())
				{
					Vector<String> kRandomWords = Operations.getKWordsFromDictionary();					
					Vector<String> kRandomWordsTranslated = Operations.translateKWords(kRandomWords);

			    	ServerSocketChannel serverSocketChannel = openServerSocketChannel();
					Selector selectorSfida = openNewTCPSfidaWithSelector(serverSocketChannel);
					
					try { runSfida(selectorSfida, serverSocketChannel, kRandomWords, kRandomWordsTranslated); }
					catch (IOException e) { e.printStackTrace(); }
					finally
					{
						MainClassWQServer.lockAndRemoveUsersInVectorUsersInSfida(this.request);
						
						try { serverSocketChannel.close(); } catch (IOException e) { e.printStackTrace(); }
						try { selectorSfida.close(); } catch (IOException e) { e.printStackTrace(); }
					}
				}
				break;
				
			case mostra_punteggio:
				Response response_mostra_punteggio = Operations.handle_mostra_punteggio(this.request);
				try { writeBufferInChannel(response_mostra_punteggio); } catch (IOException e) { e.printStackTrace(); }
				break;
				
			case mostra_classifica:
				Response response_mostra_classifica = Operations.handle_mostra_cLassifica(this.request);
				try { writeBufferInChannel(response_mostra_classifica); } catch (IOException e) { e.printStackTrace(); }
				break;
				
			default:
				System.out.println("Errore: comando non presente");
				break;
		
		}
	}

}
