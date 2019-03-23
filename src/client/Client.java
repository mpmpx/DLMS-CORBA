package client;

import java.io.*;
import java.nio.file.Paths;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import ImplementRemoteInterface.Library;
import resource.*;
import LibraryInterface.*;
import server.Server;


public class Client {
	private final String BAR = "-----------------------";
	
	private String[] args;
	private LibraryServer library;
	private ID userID;
	
	private Log log;
	private BufferedReader inFromUser;
	
	
	public Client(String[] args) {
		inFromUser = new BufferedReader(new InputStreamReader(System.in));
		this.args = args;
	}
	
	public void login() throws IOException {
		while (true) {
			System.out.println(BAR);
			System.out.println("LOGIN");
			System.out.print("Enter your ID: ");
			userID = new ID(inFromUser.readLine());
			
			if (userID.isManager() || userID.isUser()) {
				break;
			}
			else {
				System.out.println("Invalid ID. Please try again.");
			}
		}
	}
	
	private void createLog() {
		String rootDir = (Paths.get("").toAbsolutePath().toString());
		String[] path = {rootDir, "src", "client", "logs", userID.toString() + ".txt" };
		log = new Log(path);
		log.write(userID + " logged in.");
	}
	
	private void connectToServer() throws Exception {
		try {
			ORB orb = ORB.init(args, null);
			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
			
			library = LibraryServerHelper.narrow(ncRef.resolve_str(userID.getLibraryName()));
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}

		log.write("Successfully connected to library.");
	}
	
	private void addItem() throws Exception {
		System.out.println(BAR);
		System.out.println("ADD AN ITEM");
		System.out.print("Enter the ID of the item: ");
		ID itemID = new ID(inFromUser.readLine());
		System.out.print("Enter the name of the item: ");
		String itemName = inFromUser.readLine();
		System.out.print("Enter the quantity of the item you want to add: ");
		int quantity = Integer.parseInt(inFromUser.readLine());

		String msg = library.addItem(userID, itemID, itemName, quantity);
		System.out.println(msg);
		log.write(msg);
	}
	
	private void removeItem() throws Exception {
		System.out.println(BAR);
		System.out.println("REMOVE AN ITEM");
		System.out.print("Enter the ID of the item: ");
		ID itemID = new ID(inFromUser.readLine());
		System.out.print("Enter the quantity of the item you want to remove: ");
		int quantity = Integer.parseInt(inFromUser.readLine());
		
		String msg = library.deleteItem(userID, itemID, quantity);
		System.out.println(msg);
		log.write(msg);
	}
	
	private void listItem() throws Exception {
		System.out.println(BAR);
		System.out.println("LIST ALL ITEMS");
		String msg = userID + " listed all items in the library.\r\n\r\n";
		msg += library.listItemAvailability(userID);
		System.out.print(msg);
		System.out.println();
		log.write(msg);
	}
	
	private void borrowItem() throws Exception {
		System.out.println(BAR);
		System.out.println("BORROW AN ITEM");
		System.out.print("Enter the ID of the item: ");
		ID itemID = new ID(inFromUser.readLine());
		System.out.print("Enter number of days you want to borrow this book: ");
		int numberOfDay = Integer.parseInt(inFromUser.readLine());
		String msg = library.borrowItem(userID, itemID, numberOfDay);
		
		if (msg.equals(itemID + " is not availbale now. Do you want to be added in the waiting list?")) {
			System.out.print(msg + "(y/n): ");
			String choice = inFromUser.readLine();
			if (choice.equals("y")) {
				msg = library.addWaitList(userID, itemID);
			}
			else {
				msg = userID + " failed to borrow the item (ID: " + itemID + ") from the library.";
			}
		}
		System.out.println(msg);
		log.write(msg);
	}
	
	private void findItem() throws Exception {
		System.out.println(BAR);
		System.out.println("FIND AN ITEM");
		System.out.print("Enter the name of the item: ");
		String itemName = inFromUser.readLine();
		String msg = userID + " tried to find the item(Name: " + itemName + ")\r\n\r\n";
		
		msg += library.findItem(userID, itemName, true);
		System.out.println(msg);
		log.write(msg);
	}
	
	private void returnItem() throws Exception {
		System.out.println(BAR);
		System.out.println("RETURN AN ITEM");
		System.out.print("Enter the ID of the item: ");
		ID itemID = new ID(inFromUser.readLine());
		String msg = library.returnItem(userID, itemID);
		
		System.out.println(msg);
		log.write(msg);
	}
	
	private void exchangeItem() throws Exception {
		System.out.println(BAR);
		System.out.println("EXCHANGE AN ITEM");
		System.out.print("Enter the ID of the old item: ");
		ID oldItemID  = new ID(inFromUser.readLine());
		System.out.print("Enter the ID of the new item: ");
		ID newItemID  = new ID(inFromUser.readLine());
		String msg = library.exchangeItem(userID, oldItemID, newItemID);
		
		System.out.println(msg);
		log.write(msg);
	}
	
	private void menu() throws Exception {
		String userChoice;
		if (userID.isManager()) {
			while (true) {
				System.out.println(BAR);
				System.out.println("MANAGER MENU");
				System.out.println("1. Add an item");
				System.out.println("2. Remove an item");
				System.out.println("3. List all items");
				System.out.println("4. Exit");
				System.out.print("Select an operation (1-4): ");

				userChoice = inFromUser.readLine();
				switch (userChoice) {
					case "1" : addItem(); break;
					case "2" : removeItem(); break;
					case "3" : listItem(); break;
					case "4" : 
						log.write(userID + " logged out."); 
						System.out.println("You have logged out.");
						return;
					default  : System.out.println("Invalid choice. Please try again."); break;
				}
			}
		}
		else {
			while (true) {
				System.out.println(BAR);
				System.out.println("USER MENU");
				System.out.println("1. Borrow an item");
				System.out.println("2. Find an item");
				System.out.println("3. Return an item");
				System.out.println("4. Exchange an item");
				System.out.println("5. Exit");
				System.out.print("Select an operation (1-4): ");

				userChoice = inFromUser.readLine();
				switch (userChoice) {
					case "1" : borrowItem(); break;
					case "2" : findItem(); break;
					case "3" : returnItem(); break;
					case "4" : exchangeItem(); break;
					case "5" : 
						log.write(userID + " logged out."); 
						System.out.println("You have logged out.");
						return;
					default  : System.out.println("Invalid choice. Please try again."); break;
				}
			}
		}
	}
	
	private void run() {
		try {
			login();
			createLog();
			connectToServer();
			menu();
		}
		catch (Exception e) {
			e.printStackTrace();
			log.write("Error: " + e);
			log.close();
		}
	}
	
	public static void main(String args[]) throws Exception	{
		
		Client client = new Client(args);
		client.run();
	}
}
