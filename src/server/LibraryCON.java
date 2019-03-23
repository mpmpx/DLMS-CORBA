package server;

public class LibraryCON {
	public static void main(String[] args) {
		Server CONServer = new Server(args, Server.CON_PORT);
		CONServer.initBook("CON6231", "Distributed System Design", 4);
		CONServer.initBook("CON6421", "Compiler Design", 2);
		CONServer.initBook("CON6521", "Advanced Database Technology and Applications", 10);
		CONServer.initBook("CON6651", "Algorithm Design Techniques", 7);
		new Thread(CONServer).start();
	}
}
