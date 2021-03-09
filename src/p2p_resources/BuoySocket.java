package p2p_resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class BuoySocket implements SocketInterface {
	private final Socket s;
	private final InputStream is;
	private final OutputStream os;

	public BuoySocket(String host, int port) throws IOException {
		this(new Socket(host, port));
	}


	public BuoySocket(Socket socket) throws IOException {
		s = socket;
		is = s.getInputStream();
		os = s.getOutputStream();		
	}

	public Socket getBuoySocket(){
		return s;
	}

	public void close() throws IOException {
		is.close();
		os.close();
		s.close();
	}

	public int read() throws IOException {
		return is.read();
	}

	public int read(byte[] b) throws IOException {
		return is.read(b);
	}

	public void write(byte[] b) throws IOException {
		os.write(b);
		os.flush();
	}

}
