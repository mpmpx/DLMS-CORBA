package server;

public class LibraryMON {
	public static void main(String[] args) {
		Server MONServer = new Server(args, Server.MON_PORT);
		MONServer.initBook("MON6231", "Distributed System Design", 5);
		MONServer.initBook("MON6521", "Advanced Database Technology and Applications", 1);
		MONServer.initBook("MON6651", "Algorithm Design Techniques", 4);
		new Thread(MONServer).start();
	}
}
