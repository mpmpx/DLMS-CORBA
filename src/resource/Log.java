package resource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Date;

public class Log {
	private File file;
	private BufferedWriter writer;
	
	public Log (String[] path) {
		String fileSeparator = System.getProperty("file.separator");
		String fileName = "";
		
		for (int i = 0; i < path.length - 1; i++) {
			fileName += path[i];
			fileName += fileSeparator;
		}
		
		File file = new File(fileName);
		if (!file.exists()) {
			file.mkdirs();
		}
		
		fileName += path[path.length - 1];
		initLog(fileName);
	}
	
	public Log(String fileName) {
		initLog(fileName);
	}
	
	private void initLog(String fileName) {
		file = new File(fileName);
		
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			writer = new BufferedWriter(new FileWriter(file.getAbsolutePath(), true));
		}
		catch (FileNotFoundException e) {
			System.out.println("Warning: \"" + fileName + "\" cannot be found.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Warning: \"" + fileName + "\" cannot be opened.");
			e.printStackTrace();
		}
		
	}
	
	private String timeStamp() {
		return new Timestamp(new Date().getTime()).toString();
	}
	
	public void write(String msg) {
		try {
			writer.append("[" + timeStamp() + "]: " + msg);
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			System.out.println("Warning: \"" + file.getName() + "\" cannot be written.");
			e.printStackTrace();
		}

	}
	
	public void close() {
		try {
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Warning: \"" + file.getName() + "\" cannot be closed." );
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Log log = new Log(new String[] {(Paths.get("").toAbsolutePath().toString()), "src", "server", "logs", "log.txt"});
		
		log.write("first message.");
	}
}
