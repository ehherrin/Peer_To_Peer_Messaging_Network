package p2p_resources;

import java.io.IOException;

public interface SocketInterface {
	void write(byte[] b) throws IOException;
	int read() throws IOException;
	int read(byte[] b) throws IOException;
	void close() throws IOException;
}
