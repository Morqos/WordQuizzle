package operations;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.json.simple.JSONObject;

import communication.Request;
import threadpool.*;

public class Registra_utente_Remote extends UnicastRemoteObject implements Registra_utente{  
	public Registra_utente_Remote() throws RemoteException{  
		super();  
	}
	
	
	@Override
	public boolean registra_utente(String nickname, String password) throws RemoteException {

		JSONObject newUser = Operations.generateJSONUser(nickname, password);
		
		return (JSONWindow.insertNewUser(newUser));
	}  
}