package server;

public class LibraryMCG {
	public static void main(String[] args) {
		Server MCGServer = new Server(args, Server.MCG_PORT);
		MCGServer.initBook("MCG6231", "Distributed System Design", 2);
		MCGServer.initBook("MCG6521", "Advanced Database Technology and Applications", 3);
		new Thread(MCGServer).start();
	}
}
