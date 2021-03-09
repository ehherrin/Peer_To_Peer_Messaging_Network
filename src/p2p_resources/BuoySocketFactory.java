package p2p_resources;

import java.io.IOException;
import java.net.Socket;

public class BuoySocketFactory {

	public static BuoySocket makeSocket(String host, int port) throws IOException {
		return new BuoySocket(host, port);
	}

	public static BuoySocket makeSocket(Socket socket) throws IOException {
		return new BuoySocket(socket);
	}

}
