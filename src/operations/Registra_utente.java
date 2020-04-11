package operations;

import java.rmi.Remote;
import java.rmi.RemoteException;

import communication.Request;
import threadpool.*;

public interface Registra_utente extends Remote{ 
	String SERVICE_NAME = "Registra_utente_Service";
	public boolean registra_utente(String nickname, String password)throws RemoteException;  
}