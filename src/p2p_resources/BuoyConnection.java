package p2p_resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class BuoyConnection {
	private final BuoyInfo buoy_peer_data;
	private BuoySocket buoy_socket;
	
	public BuoyConnection(BuoyInfo peer_info) throws IOException {
		buoy_peer_data = peer_info;
		buoy_socket = BuoySocketFactory.makeSocket(buoy_peer_data.getHost(), buoy_peer_data.getPort());

	}
	
	
	public BuoyConnection(BuoyInfo peer_info, BuoySocket socket_info) {
		buoy_peer_data = peer_info;
		buoy_socket = socket_info;
	}
	
	
	public void sendData(BuoyPeerMessage peer_message_object) {
		try {
//			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream objStream = new ObjectOutputStream(buoy_socket.getBuoySocket().getOutputStream());
			objStream.writeObject(peer_message_object);
			objStream.flush();
			buoy_socket.getBuoySocket().getOutputStream().flush();
//			objStream.close();
//			buoy_socket.write(byteStream.toByteArray());
		}
		catch (IOException e) {
			System.err.println(e.toString());
		}
	}
	
	
	public BuoyPeerMessage recvData() {
		try {
			return new BuoyPeerMessage(buoy_socket);
		}
		catch (IOException | ClassNotFoundException e) {
			System.err.println(e.toString());
			return null;
		}
	}
	
	
	public void close() {
		if (buoy_socket != null) {
			try {
				buoy_socket.close();
			} catch (IOException ignored) {
			}
			buoy_socket = null;
		}
	}


	public String toString() {
		return "PeerConnection[" + buoy_peer_data + "]";
	}
	
}
