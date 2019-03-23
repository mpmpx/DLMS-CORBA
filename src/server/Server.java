package server;

import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.rmi.RemoteException;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import ImplementRemoteInterface.Library;
import resource.*;
import LibraryInterface.*;

public class Server implements Runnable {
	
	public static final int CON_PORT = 4000;
	public static final int MCG_PORT = 4010;
	public static final int MON_PORT = 4020;
	public static final int RMI_OFFSET = 1;
	

	public Log log;
	private int port;
	private String[] args;
	private ServerSocket serverSocket;
	protected Library library;
	protected LibraryServer libraryServer;
	protected LibraryName libraryName;
	

	public void initBook(String itemID, String itemName, int quantity) {
		library.initBook(itemID, itemName, quantity);
	}
	
	public Server(String[] args, int port) {
		this.port = port;
		this.args = args;
		
		switch (port) {
			case CON_PORT : 
				libraryName = LibraryName.CON; break;
			case MCG_PORT : 
				libraryName = LibraryName.MCG; break;
			case MON_PORT : 
				libraryName = LibraryName.MON; break;
		}
		
		createLog();
		library = new Library(this, libraryName);
	}
	
	private void createLog() {
		String rootDir = (Paths.get("").toAbsolutePath().toString());
		String[] path = {rootDir, "src", "server", "logs", libraryName.toString() + "_server_log.txt"};
		log = new Log(path);
	}
	
	public void run() {
		try {
			// create and initialize the ORB //// get reference to rootpoa &amp; activate
			// the POAManager
			ORB orb = ORB.init(args, null);
			// -ORBInitialPort 1050 -ORBInitialHost localhost
			POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			rootpoa.the_POAManager().activate();
			
			// create servant and register it with the ORB
			
			
			// get object reference from the servant
			org.omg.CORBA.Object ref = rootpoa.servant_to_reference(library);
			libraryServer = LibraryServerHelper.narrow(ref);
			
			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
			
			NameComponent path[] = ncRef.to_name(libraryName.toString());
			ncRef.rebind(path,libraryServer);
			System.out.println("Server(" + libraryName + ") is Started.");
			log.write("Server(" + libraryName + ") is Started.");
			serverSocket = new ServerSocket(port);
			
			new Thread(new ORBThread(orb)).start();
			while (true) {
				Socket clientSocket = serverSocket.accept();
				new Thread(new ClientHandler(this, clientSocket)).start();
			}
			
			
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
		} 
	}
	
	public static void main(String[] args) {
		Server CONServer = new Server(args, CON_PORT);
		Server MCGServer = new Server(args, MCG_PORT);
		Server MONServer = new Server(args, MON_PORT);
		
		CONServer.initBook("CON6231", "Distributed System Design", 4);
		CONServer.initBook("CON6421", "Compiler Design", 2);
		CONServer.initBook("CON6521", "Advanced Database Technology and Applications", 10);
		CONServer.initBook("CON6651", "Algorithm Design Techniques", 7);

		MCGServer.initBook("MCG6231", "Distributed System Design", 2);
		MCGServer.initBook("MCG6521", "Advanced Database Technology and Applications", 3);
		
		MONServer.initBook("MON6231", "Distributed System Design", 5);
		MONServer.initBook("MON6521", "Advanced Database Technology and Applications", 1);
		MONServer.initBook("MON6651", "Algorithm Design Techniques", 4);
		
		new Thread(CONServer).start();
		new Thread(MCGServer).start();
		new Thread(MONServer).start();
	}
}

class ORBThread implements Runnable {
	private ORB orb;
	
	public ORBThread(ORB orb) {
		this.orb = orb;
	}
	
	@Override
	public void run() {
		orb.run();
	}
	
}

class ClientHandler implements Runnable {
	
	private Server server;
	private Socket socket;
	
	public ClientHandler(Server server, Socket clientSocket) {
		this.server = server;
		socket = clientSocket;
	}
	
	@Override
	public void run() {
		try {
			BufferedReader inFromClient = 
						new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String clientMsg = inFromClient.readLine();
			String replyMsg = "";
			String[] splitMsg = clientMsg.split(";");
			DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
				
			switch (splitMsg[0]) {
				case "Borrow" :{
					replyMsg = server.library.borrowItem(new ID(splitMsg[1]), new ID(splitMsg[2]), Integer.parseInt(splitMsg[3]));
					outToClient.writeBytes(replyMsg + "\n");
					break;
				}
				case "Find" : {
					replyMsg = server.library.findItem(new ID(splitMsg[1]), splitMsg[2], false);
					outToClient.writeBytes(replyMsg + "\n");
					break;
				}
				case "Return" : {
					replyMsg = server.library.returnItem(new ID(splitMsg[1]), new ID(splitMsg[2]));
					outToClient.writeBytes(replyMsg + "\n");
					break;
				}
				case "AddList" : {
					replyMsg = server.library.addWaitList(new ID(splitMsg[1]), new ID(splitMsg[2]));
					outToClient.writeBytes(replyMsg + "\n");
					break;
				}
				case "HasBorrowed" : {
					replyMsg = server.library.hasBorrowed(new ID(splitMsg[1]), new ID(splitMsg[2]));
					outToClient.writeBytes(replyMsg + "\n");
					break;
				}
				default : break;
			}
				
		} catch (IOException e) {
			e.printStackTrace();
		}
 	}
}
