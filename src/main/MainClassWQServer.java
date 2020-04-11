package main;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import communication.Request;
import operations.*;
import threadpool.*;


public class MainClassWQServer {
	
	private static ServerSocketChannel serverSocketChannel;
	private static InetSocketAddress hostAddress;
	public static final String pathName = "https://api.mymemory.translated.net/get?";
	public static final String tagWord = "q=";
	public static final String tagLangPair = "langpair=it|en";
	public static final int timeoutUDPResponse = 30000;
	public static final int timeoutSfida = 40000; //reset at 40000
	public static final int portServer = 9876;
	public static DatagramSocket datagramSocket;
	public static final int nWordsInDictionary = 1000;
	public static final int KRandomWordsFromDictionary = 8;
	public static final int pointsCorrectTranslation = 2;
	public static final int pointsWrongTranslation = -1;
	public static final int pointsWinnerSfida = 3;
	public static final int nBytesInBuffer = 1024;
    private static Selector selector;
    private static SelectionKey selectKey;
    private static ThreadPool threadpool;
    static JSONWindow jsonWindow;
	public static HashMap<SocketChannel, String> usersOnline;
	public static ReentrantLock lockUsersOnline = new ReentrantLock();
	public static Vector<String> usersInSfida;
	public static ReentrantLock lockUsersInSfida = new ReentrantLock();
	
	public synchronized static void addUserInVectorUsersInSfida(String nicknameUser) throws NullPointerException
	{
		if(nicknameUser==null) throw new NullPointerException();
		usersInSfida.add(nicknameUser);
	}
	public synchronized static void removeUserInVectorUsersInSfida(String nicknameUser) throws NullPointerException
	{
		if(nicknameUser==null) throw new NullPointerException();
		usersInSfida.remove(nicknameUser);
	}
	public static void lockAndRemoveUsersInVectorUsersInSfida(Request request)
	{
		MainClassWQServer.lockUsersInSfida.lock();
		MainClassWQServer.removeUserInVectorUsersInSfida(request.getNickname());
		MainClassWQServer.removeUserInVectorUsersInSfida(request.getFriend());
		MainClassWQServer.lockUsersInSfida.unlock();
	}
    
    
    private static void handleRequest(Request request, SocketChannel clientSocketChannel) throws IOException
    {
    	if(request==null || !clientSocketChannel.isOpen()) return;
    	
        Runnable worker = new WorkerThread(request, clientSocketChannel);
        threadpool.executeTask(worker);
    }
    
    
    private static void configureSelectorAndServer() throws IOException
    {
    	selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        hostAddress = new InetSocketAddress("localhost", 8080);
        serverSocketChannel.bind(hostAddress);  
        serverSocketChannel.configureBlocking(false); 
        int validOps = serverSocketChannel.validOps();  
        selectKey = serverSocketChannel.register(selector, validOps, null);  
    }
    
    
    private static Request readRequest(SocketChannel clientSocketChannel)
    {
    	Request request = new Request(null, null, null, null);
    	
        ByteBuffer requestBuffer = ByteBuffer.allocate(nBytesInBuffer);  
        try { clientSocketChannel.read(requestBuffer); }
        catch (IOException e1) { e1.printStackTrace(); return null; }
        
        String requestString = new String(requestBuffer.array()).trim();
        try {
			JSONObject jsonObject = (JSONObject) new JSONParser().parse(requestString);
			request.fromJSONObjectToRequest(jsonObject);
		} catch (ParseException e) { e.printStackTrace(); }
        
        return request;
    }
    
    
    private static void configureBlockingAndRegisterClient(SocketChannel client) throws IOException {
        client.configureBlocking(false);  // The new connection is added to a selector  
        client.register(selector, SelectionKey.OP_READ);  
        
        System.out.println("The new connection is accepted from the client: " + client);  
    }
    
    
	private static void launchServer() throws IOException, ClassNotFoundException
	{
		configureSelectorAndServer();
		
        while(true){
        	int numberOfKeys = selector.select();
        	System.out.println("The Number of selected keys are: " + numberOfKeys);  

            Set selectedKeys = selector.selectedKeys();
            Iterator iteratorKeys = selectedKeys.iterator();
            
            while (iteratorKeys.hasNext()) {
                SelectionKey key = (SelectionKey) iteratorKeys.next();
                if (key.isAcceptable()) {
                    SocketChannel client = serverSocketChannel.accept(); // The new client connection is accepted  
                    configureBlockingAndRegisterClient(client);
                }
                else if (key.isReadable()) {
                    SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                    Request request = readRequest(clientSocketChannel);

                    if(request==null)
                    {
                    	Operations.lockAndRemoveUserOnline(clientSocketChannel);
                    	clientSocketChannel.close();
                	}
                    else handleRequest(request, clientSocketChannel);
                }
                iteratorKeys.remove();
            }
        }
        
	}
	
	
	private static void instantiateAndLaunchThreadPool(int numberWorkingThreads) {
		threadpool = new ThreadPool(numberWorkingThreads);
		threadpool.launchThreadPool();
	}
	
	
	private static void openRMIinterface() {
		try {
			Registra_utente stub_registra_utente = new Registra_utente_Remote();
			LocateRegistry.createRegistry(1099);
			Registry r = LocateRegistry.getRegistry(1099);
			r.rebind(stub_registra_utente.SERVICE_NAME, stub_registra_utente);
			System.out.println("In openRMIinterface");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void initializeDatagramSocketServer()
	{
		 try { datagramSocket = new DatagramSocket(portServer); }
		 catch (SocketException e) { e.printStackTrace(); }
	}
	
	
	public static void main(String[] args) throws IOException, ClassNotFoundException
	{
		try {jsonWindow = new JSONWindow();} catch (Exception e) {e.printStackTrace();}
		usersOnline = new HashMap<>();
		usersInSfida = new Vector<String>();
		
		openRMIinterface();
		initializeDatagramSocketServer();
		
		instantiateAndLaunchThreadPool(10);
		launchServer();
	}

}
