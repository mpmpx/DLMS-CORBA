package resource;

import java.io.Serializable;
import java.util.LinkedList;
import LibraryInterface.*;

public class Book implements Serializable {
	private ID itemID;
	private String name;
	private int totalQuantity;
	private int availableQuantity;
	private LinkedList<String> borrowRecord;
	
	public Book(ID itemID, String name, int quantity) {
		this.itemID = itemID;
		this.name = name;
		totalQuantity = quantity;
		availableQuantity = totalQuantity;
		borrowRecord = new LinkedList<String>();
	}
	
	public void add(int quantity) {
		totalQuantity += quantity;
		availableQuantity += quantity;
	}
	
	public void remove(int quantity) {
		if (availableQuantity >= quantity) {
			totalQuantity -= quantity;
			availableQuantity -= quantity;
		}
	}
	
	public void borrow(String userID) {
		borrowRecord.add(userID);
		availableQuantity--;
	}
	
	public boolean hasBorrowed(String userID) {
		return borrowRecord.contains(userID);
	}
	
	public boolean returnBook(String userID) {
		if (borrowRecord.contains(userID)) {
			borrowRecord.remove(userID);
			availableQuantity++;
			return true;
		}
		return false;
	}
	
	public ID getID() {
		return itemID;
	}
	
	public String getName() {
		return name;
	}
	
	public int getAvailableQuantity() {
		return availableQuantity;
	}
	
	public int getTotalQuantity() {
		return totalQuantity;
	}
	
	public String toString() {
		return itemID + " " + name + " " + availableQuantity;
	}
}
