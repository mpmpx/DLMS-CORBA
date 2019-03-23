package ImplementRemoteInterface;

import java.io.*;
import java.net.*;
import java.util.*;

import org.omg.CORBA.ORB;

import LibraryInterface.*;
import LibraryInterface.ID;
import resource.*;
import server.Server;

public class Library extends LibraryServerPOA {

	private LibraryName name;
	private Server server;
	private ORB orb;
	private HashMap<String, Book> repository;
	private HashMap<String, LinkedList<String>> waitingList;
	private HashMap<String, List<String>> borrowRecord;
	private LinkedList<String> externalStudentRecord;
	
	public Library(Server server, LibraryName name) {
		super();
		
		this.name = name;
		this.server = server;
		repository = new HashMap<String, Book>();
		waitingList = new HashMap<String, LinkedList<String>>();
		borrowRecord = new HashMap<String, List<String>>();
		externalStudentRecord = new LinkedList<String>();
	}
	
	public void initBook(String itemID, String itemName, int quantity) {
		Book book = new Book(new ID(itemID), itemName, quantity);
		repository.put(book.getID().toString(), book);
	}
	
	@Override
	public synchronized String addItem(ID managerID, ID itemID, String itemName, int quantity) {
		String msg = "";
		if (!itemID.isItem() || !itemID.getLibraryName().equals(name.toString())) {
			msg = managerID + " failed to add " + quantity + " book(s) (ID: " + itemID + ", name: "
					+ itemName + ") because of invalid itemID.";
		}
		else if (repository.containsKey(itemID.toString())) {
			if (!repository.get(itemID.toString()).getName().equals(itemName)) {
				msg = managerID + " failed to add " + quantity + " book(s) (ID: " + itemID + ", name: "
						+ itemName + ") because of invalid itemID.";
			} else {
				repository.get(itemID.toString()).add(quantity);
				msg = managerID + " added " + quantity + " book(s) (ID: " + itemID + ", name: " + itemName
						+ ") in the library.";
				server.log.write(msg);
				removeFromWaitList(itemID);
				return msg;
			}
		} else {
			repository.put(itemID.toString(), new Book(itemID, itemName, quantity));
			msg = managerID + " created and added " + quantity + " book(s) (ID: " + itemID + ", name: "
					+ itemName + ") in the library.";
		}
		
		server.log.write(msg);
		return msg;
	}

	@Override
	public synchronized String deleteItem(ID managerID, ID itemID, int quantity) {
		String msg = "";
		
		if (!repository.containsKey(itemID.toString())) {
			msg = managerID + " failed to remove " + quantity + " book(s) (ID: " + itemID + ") in the library. No such item.";
		} else {
			int availableQuantity = repository.get(itemID.toString()).getAvailableQuantity();
			if (quantity < 0) {
				// remove the total existence of the item.
				if (quantity == -1) {
					repository.remove(itemID.toString());
					waitingList.remove(itemID.toString());
					msg = managerID + " successfully removed the total existence of the item (ID: " + itemID
							+ ") from the library.";
				}
				else {
					msg = managerID + " failed to remove the item (ID: " + itemID
							+ ") from the library because of invalid input " + quantity + ".";
				}
			}
			// if the number of available item is greater or equal to the number given by the manager. 
			else if (quantity <= availableQuantity) {
				repository.get(itemID.toString()).remove(quantity);
				msg = managerID + " successfully removed " + quantity + " book(s) (ID: " + itemID
						+ ") from the library.";
			} else {
				msg = managerID + " failed to remove the item (ID: " + itemID + ", available quantity: "
						+ availableQuantity + ") from the library by removing " + quantity + " of them.";
			}
		}
		
		server.log.write(msg);
		return msg;
	}

	@Override
	public synchronized String listItemAvailability(ID managerID) {
	
		String msg = "";
		for (Book book : repository.values()) {
			msg += book.toString() + "\r\n";
		}
		
		server.log.write(managerID + " listed all items in the library.");
		return msg;
	}
	@Override
	public synchronized String borrowItem(ID userID, ID itemID, int numberOfDay) {
		String msg = "";
		
		// If the item belongs to neither of libraries.
		if (!itemID.isItem()) {
			msg = userID + " failed to borrow the book(ID: " + itemID + ") for " + numberOfDay + " day(s) because the item does not exist.";
			server.log.write(msg);
			return msg;
		}
		
		try {
			// If the item belongs to the this library.
			if (itemID.getLibraryName().equals(name.toString())) {
				
				// If the item is kept in the library.
				if (repository.containsKey(itemID.toString())) {
					// If the item is available in the library.
					Book book = repository.get(itemID.toString());
					if (book.getAvailableQuantity() > 0) {
						
						// If a student who is not from this library wants to borrow a book,
						// check whether he has borrowed a book from this library or not.
						
						if (!userID.getLibraryName().equals(name.toString())) {
							if (externalStudentRecord.contains(userID.toString())) {
								msg = userID + " failed to borrow the book(ID: " + itemID + ") from the library " + name
										+ " because he has already borrowed one.";
								server.log.write(msg);
								return msg;
							}
							else {
								externalStudentRecord.add(userID.toString());
							}
						}
						
						// If the student has borrowed the item.
						if (book.hasBorrowed(userID.toString())) {
							msg = userID + " failed to borrow the book(ID: " + itemID
									+ ") because he has already borrowed one.";
							server.log.write(msg);
							return msg;
						}
						
						book.borrow(userID.toString());
						msg = userID + " successfully borrowed an item(ID: " + itemID + ") for " + numberOfDay + " day(s) from library " + name + ".";
						server.log.write(msg);
					}
					// If the item is not available in the library, ask user adding in the waiting list.
					else {
						msg = itemID + " is not availbale now. Do you want to be added in the waiting list?";
					}
				}
				// If the item is not kept in the library, return message of failing operation.
				else {
					msg = userID + " failed to borrow the book(ID: " + itemID + ") for " + numberOfDay + " day(s) because the item does not exist.";
					server.log.write(msg);
				}
			}
			
			// If the item belongs to other libraries.
			else {
				int port = 0;
				switch (itemID.getLibraryName()) {
					case "CON": port = Server.CON_PORT; break;
					case "MCG": port = Server.MCG_PORT; break;
					case "MON": port = Server.MON_PORT; break;
				}
				
				Socket clientSocket = new Socket("localhost", port);
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				outToServer.writeBytes("Borrow;" + userID + ";" + itemID + ";" + numberOfDay + "\n");
				
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				msg = inFromServer.readLine();
				clientSocket.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
			server.log.write("Error: " + e);
		}
		
		return msg;
	}

	@Override
	public synchronized String findItem(ID userID, String itemName, boolean recursive) {
		String msg = "";
		
		server.log.write(userID + " tried to find the item(Name: " + itemName + ") in the library.");
		for (Book book : repository.values()) {
			if (book.getName().equals(itemName)) {
				msg += (book.getID() + " " + book.getName() + " " + book.getAvailableQuantity() + ";");
			}
		}
		
		if (!recursive) {
			return msg;
		}
		else {
			String[] splitMsg = msg.split(";");
			msg = "";
			for (String eachMsg : splitMsg) {
				msg += (eachMsg + "\r\n");
			}
		}
		
		int[] serverPort = new int[2];
		switch (name) {
			case CON : serverPort = new int[] {Server.MCG_PORT, Server.MON_PORT}; break;
			case MCG : serverPort = new int[] {Server.CON_PORT, Server.MON_PORT}; break;
			case MON : serverPort = new int[] {Server.CON_PORT, Server.MCG_PORT}; break;
		}
		
		
		for (int port : serverPort) {
			Socket clientSocket;
			try {
				clientSocket = new Socket("localhost", port);
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				outToServer.writeBytes("Find;" + userID + ";" + itemName + "\n");

				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

				String[] splitMsg = inFromServer.readLine().split(";");
				for (String eachMsg : splitMsg) {
					msg += (eachMsg + "\r\n");
				}
				clientSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
				server.log.write("Error: " + e);
			}
		}
		return msg;
	}

	@Override
	public synchronized String returnItem(ID userID, ID itemID) {
		String msg = "";
		
		// If the item is invalid;
		if (!itemID.isItem()) {
			msg = userID + " failed to return the book(ID: " + itemID + ") because the item does not exist.";
			server.log.write(msg);
			return msg;
		}
		
		try {
			// If the item belongs to this library.
			if (itemID.getLibraryName().equals(name.toString())) {
				// If the item is kept in the library.
				if (repository.containsKey(itemID.toString())) {
					Book book = repository.get(itemID.toString());
					// If the book is borrowed by the person who is returning it to the library.
					if (book.returnBook(userID.toString())) {
						externalStudentRecord.remove(userID.toString());
						msg = userID + " successfully returned the book(ID: " + itemID + ").";
						server.log.write(msg);
						removeFromWaitList(itemID);
						return msg;
					}
					else {
						msg = userID + " failed to return the book(ID: " + itemID + ") because he did not borrow this book.";
						server.log.write(msg);
						return msg;
					}
				}
				else {
					msg = userID + " failed to return the book(ID: " + itemID + ") because the item does not exist.";
					server.log.write(msg);
					return msg;
				}
			}
			else {
				int port = 0;
				switch (itemID.getLibraryName()) {
					case "CON": port = Server.CON_PORT; break;
					case "MCG": port = Server.MCG_PORT; break;
					case "MON": port = Server.MON_PORT; break;
				}
				
				Socket clientSocket = new Socket("localhost", port);
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				outToServer.writeBytes("Return;" + userID + ";" + itemID + "\n");
				
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				msg = inFromServer.readLine();
				clientSocket.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			server.log.write("Error: " + e);
		}
	
		return msg;
	}
	
	public synchronized String exchangeItem(ID userID, ID oldItemID, ID newItemID) {
		String msg = "";
		try {
			// test whether the user borrowed the item
			if (oldItemID.getLibraryName().equals(name.toString())) {
				msg = hasBorrowed(userID, oldItemID);
			}
			else {
				int port = 0;
				switch (oldItemID.getLibraryName()) {
					case "CON": port = Server.CON_PORT; break;
					case "MCG": port = Server.MCG_PORT; break;
					case "MON": port = Server.MON_PORT; break;
					default   : return "Error: server " + oldItemID.getLibraryName() + "is not found";
				}
				Socket clientSocket = new Socket("localhost", port);
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				outToServer.writeBytes("HasBorrowed;" + userID + ";" + oldItemID + "\n");
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				msg = inFromServer.readLine();
				clientSocket.close();
			}
				
			if (!msg.substring(0, 4).equals("True")) {
				msg = userID + " failed to exchange the Item(ID: " + oldItemID + ") for the other Item(ID: " + newItemID + ") "
						+ "because " + msg;
				return msg;
			}
			
			// test if the new item is available
			
			msg = borrowItem(userID, newItemID, 10);
			if (!msg.substring(9, 21).equals("successfully")) {
				msg = userID + " failed to exchange the Item(ID: " + oldItemID + ") for the other Item(ID: " + newItemID + ") "
						+ "because item(ID: " + newItemID + ") is not available.";
				return msg;
			}
			
			returnItem(userID, oldItemID);
			msg = userID + " successfully exchanged the Item(ID: " + oldItemID + ") for the other Item(ID: " + newItemID + "). ";
			
			return msg;
		}
		catch (Exception e) {
			server.log.write("Error: " + e);
		}
		return  msg;
	}
	
	public synchronized String hasBorrowed(ID userID, ID itemID) {
		String msg = "";
		if (!repository.containsKey(itemID.toString())) {
			msg = "item(ID: " + itemID +") is invalid.";
		}
		else {
			Book book = repository.get(itemID.toString());
			if (book.hasBorrowed(userID.toString())) {
				msg = "True";
			}
			else {
				msg = "item(ID: " + itemID +") was not lent to the user(ID: " + userID +").";
			}
		}
		return msg;
	}
	
	public synchronized String addWaitList(ID userID, ID itemID) {
		String msg = "";
		// If the item belongs to this library.
		try {
			if (itemID.getLibraryName().equals(name.toString())) {
				if (waitingList.containsKey(itemID.toString())) {
					LinkedList<String> queue = waitingList.get(itemID.toString());
					queue.add(userID.toString());
				} else {
					LinkedList<String> queue = new LinkedList<String>();
					queue.add(userID.toString());
					waitingList.put(itemID.toString(), queue);
				}
				msg = userID + " was successfully added in the waiting list for the item(ID: " + itemID + ").";
				server.log.write(msg);
			} else {
				int port = 0;
				switch (itemID.getLibraryName()) {
				case "CON":
					port = Server.CON_PORT;
					break;
				case "MCG":
					port = Server.MCG_PORT;
					break;
				case "MON":
					port = Server.MON_PORT;
					break;
				}

				Socket clientSocket = new Socket("localhost", port);
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				outToServer.writeBytes("AddList;" + userID + ";" + itemID + "\n");
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				msg = inFromServer.readLine();
				clientSocket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			server.log.write("Error: " + e);
		}
	
		return msg;
	}

	private void removeFromWaitList(ID itemID) {
		Book book = repository.get(itemID.toString());
		
		// If the item does not have a waiting list.
		if (!waitingList.containsKey(itemID.toString())) {
			return;
		}

		while (waitingList.containsKey(itemID.toString()) && book.getAvailableQuantity() > 0 ) {
			String waitingUserID = waitingList.get(itemID.toString()).pop();
			if (waitingList.get(itemID.toString()).isEmpty()) {
				waitingList.remove(itemID.toString());
			}
			borrowItem(new ID(waitingUserID), itemID, 1);
		}
	}


}

