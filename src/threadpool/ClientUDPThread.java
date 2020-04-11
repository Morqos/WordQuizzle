package threadpool;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import main.*;
import operations.*;

public class ClientUDPThread implements Runnable  {
	private String nickname;
	private int clientUDPPort;
	private DatagramSocket clientSocket;
	private boolean stillInTimeToRespondToUDPRequest = true;

	public ClientUDPThread(String nickname)
	{
		this.nickname = JSONWindow.nicknameToLowerCaseString(nickname);
		this.clientUDPPort = Operations.computePort(this.nickname);
		try { this.clientSocket = new DatagramSocket(this.clientUDPPort); } catch (SocketException e) { e.printStackTrace(); }
    }
	
	public void writeUDPRequest(String responseString)
	{
		try {
			byte[] buffer=responseString.getBytes("US-ASCII");
			InetAddress address = InetAddress.getByName("Localhost");
			DatagramPacket mypacket = new DatagramPacket(buffer,buffer.length,address,MainClassWQServer.portServer);
			this.clientSocket.send(mypacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String readUDPRequest()
	{
		try
		{
			byte[] buffer = new byte[100];
			DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
			this.clientSocket.receive(receivedPacket);
			String friendNickSfidaRequest = new String(receivedPacket.getData(), 0, receivedPacket.getLength(), "US-ASCII");
			return friendNickSfidaRequest;
		}
		catch(Exception e) { e.printStackTrace(); }
		return null;
	}
	
	public boolean readAnswerSfidaRequestFromInput()
	{
		String s_or_n = null;
		do
		{
			try { s_or_n = MainClassWQClient.br.readLine(); } catch (IOException e) { e.printStackTrace(); }
			
			if(s_or_n != null)
			{
		        s_or_n = s_or_n.toLowerCase();
		        if(s_or_n.equals("s") || s_or_n.equals("n")) break;
			}
		}while(this.stillInTimeToRespondToUDPRequest);
		
		if(this.stillInTimeToRespondToUDPRequest) System.out.println("Richiesta letta: " + s_or_n);
		
		if(s_or_n.equals("s")) return true;
		else return false;
	}
	
	@Override
	public void run()
	{
		while(true)
		{
			String friendNickSfidaRequest = readUDPRequest();
			if(friendNickSfidaRequest==null) continue;
			else if(MainClassWQClient.sfidaStarted || MainClassWQClient.waitingSfidaResponse){ writeUDPRequest("n"); continue; }

			System.out.println("Richiesta di sfida da " + friendNickSfidaRequest + ", vuoi accettare? [s/n]");
			
			Timer timer = new Timer();
			timer.schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					stillInTimeToRespondToUDPRequest = false;
					System.out.println("Tempo per rispondere a richiesta sfida scaduto, inviare input qualsiasi per continuare...");				
				}
			}, MainClassWQServer.timeoutUDPResponse);
			
			
			boolean answerSfidaRequestFromInput = readAnswerSfidaRequestFromInput();
			if(this.stillInTimeToRespondToUDPRequest)
			{
				timer.cancel();
				if(!answerSfidaRequestFromInput) { writeUDPRequest("n"); }
				else
				{
					writeUDPRequest("s");
					MainClassWQClient.sfidaStarted = true;
					try { MainClassWQClient.sfidaStart(friendNickSfidaRequest); } catch (IOException e) { e.printStackTrace(); }
					MainClassWQClient.sfidaStarted = false;
				}
			}
			this.stillInTimeToRespondToUDPRequest=true;
		}
	}
	
}