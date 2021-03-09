package p2p_resources;
import javax.swing.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

public class BuoyNode {
	private static final int SOCKET_TIMEOUT = 2000;
	private BuoyInfo myInfo;
	private final int maxPeers;
	private final Hashtable<String, BuoyInfo> peers;
	private final Hashtable<String,HandlerInterface> handlers;
	private boolean shutdown;

	private class PeerHandler extends Thread {
		private final BuoySocket buoy_socket;

		public PeerHandler(Socket socket) throws IOException {
			buoy_socket = BuoySocketFactory.makeSocket(socket);
		}

		public void run() {
			BuoyConnection peer_connection = new BuoyConnection(null, buoy_socket);
			BuoyPeerMessage peer_message = peer_connection.recvData();
			System.out.println("Received a " + peer_message.getMsgType() + " request.");
			if (handlers.containsKey(peer_message.getMsgType())) {
				handlers.get(peer_message.getMsgType()).handleMessage(peer_connection, peer_message);
			}
			peer_connection.close();
		}
	}


	private static class StabilizerRunner extends Thread {
		private final StabilizerInterface stabilizer_interface_object;
		private final int delay;

		public StabilizerRunner(StabilizerInterface stabilizer_interface_object, int delay) {
			this.stabilizer_interface_object = stabilizer_interface_object;
			this.delay = delay;
		}
		// Every five seconds, check for dead buoy peers.
		public void run() {
			while (true) {
				stabilizer_interface_object.stabilizer();
				try {
					Thread.sleep(delay);
				}
				catch (InterruptedException ignored) {
				}
			}
		}
	}


	public BuoyNode(int maxPeers, BuoyInfo info) {
		if (info.getHost() == null)
			info.setHost(getHostname());
		if (info.getId() == null)
			info.setId(info.getHost() + ":" + info.getPort());
		this.myInfo = info;
		this.maxPeers = maxPeers;
		this.peers = new Hashtable<>();
		this.handlers = new Hashtable<>();
		this.shutdown = false;
	}


	private String getHostname() {
		String host = "";
		try {
			Socket s = new Socket("www.google.com", 80);
			host = s.getLocalAddress().getHostAddress();
		} catch (IOException ignored) {
		}
		return host;
	}


	public ServerSocket makeServerSocket(int port) throws IOException {
		return makeServerSocket(port, 5);
	}


	public ServerSocket makeServerSocket(int port, int backlog) throws IOException {
		ServerSocket s = new ServerSocket(port, backlog);
		s.setReuseAddress(true);
		return s;
	}


	public List<BuoyPeerMessage> connectAndSend(BuoyInfo pd, String msgtype, String msgdata, boolean waitreply) {
		List<BuoyPeerMessage> msgreply = new ArrayList<>();
		try {
			BuoyConnection peerconn = new BuoyConnection(pd);
			BuoyPeerMessage tosend = new BuoyPeerMessage(msgtype, msgdata);
			peerconn.sendData(tosend);
			if (waitreply) {
				BuoyPeerMessage onereply = peerconn.recvData();
				while (onereply != null) {
					msgreply.add(onereply);
					onereply = peerconn.recvData();
				}
			}
			peerconn.close();
		} catch (IOException ignored) {}
		return msgreply;
	}

	public List<BuoyPeerMessage> connectAndSendFile(BuoyInfo server_info, BuoyInfo target_info,
													String msgtype, byte[] msgdata, boolean waitreply) {
		List<BuoyPeerMessage> msgreply = new ArrayList<>();
		try {
			BuoyConnection peerconn = new BuoyConnection(server_info);
			BuoyPeerMessage tosend = new BuoyPeerMessage(msgtype, msgdata, target_info);
			peerconn.sendData(tosend);
			if (waitreply) {
				BuoyPeerMessage onereply = peerconn.recvData();
				while (onereply != null) {
					msgreply.add(onereply);
					onereply = peerconn.recvData();
				}
			}
			peerconn.close();
		} catch (IOException ignored) {}
		return msgreply;
	}


	public void mainLoop() {
		try {
			int initial_port_number = myInfo.getPort();
			ServerSocket server_socket = makeServerSocket(initial_port_number);
			int current_port_number = server_socket.getLocalPort();
			if (initial_port_number != current_port_number) {
				String ip_address = myInfo.getHost();
				String host_name = "CLIENT_" + current_port_number;
				myInfo = new BuoyInfo(ip_address, current_port_number, host_name);
			}
			server_socket.setSoTimeout(SOCKET_TIMEOUT);
			while (!shutdown) {
				try {
					// This will obtain the client socket once the server accepts a connection
					Socket client_socket = server_socket.accept();
					client_socket.setSoTimeout(0);
					// Responsible for allowing the Buoy to receive data and respond with the correct handling
					// behavior according to the type of message that was received.
					PeerHandler peer_handler = new PeerHandler(client_socket);
					peer_handler.start();
				}
				catch (SocketTimeoutException ignored) {}
			}
			server_socket.close();
		} catch (IOException ignored) {}
		shutdown = true;
	}


	public void startStabilizer(StabilizerInterface stabilizer_interface_object, int delay) {
		StabilizerRunner buoy_stabilizer_runner = new StabilizerRunner(stabilizer_interface_object, delay);
		buoy_stabilizer_runner.start();
	}


	public void addHandler(String message_type, HandlerInterface handler) {
		handlers.put(message_type, handler);
	}


	public void addPeer(BuoyInfo pd) {
		addPeer(pd.getId(), pd);
	}


	public void addPeer(String key, BuoyInfo pd) {
		if ((maxPeers == 0 || peers.size() < maxPeers) &&
				!peers.containsKey(key)) {
			peers.put(key, pd);
		}
	}


	public BuoyInfo getPeer(String key) {
		return peers.get(key);
	}


	public void removePeer(String key) {
		peers.remove(key);
	}


	public Set<String> getPeerKeys() {
		return peers.keySet();
	}


	public int getNumberOfPeers() {
		return peers.size();
	}


	public boolean maxPeersReached() {
		return maxPeers > 0 && peers.size() == maxPeers;
	}


	public String getId() {
		return myInfo.getId();
	}


	public void setId(String id){
		myInfo.setId(id);
	}


	public String getHost() {
		return myInfo.getHost();
	}

	public int getMaxPeers(){
		return maxPeers;
	}

	public void setMyInfo(BuoyInfo newInfo){
		myInfo = newInfo;
	}

	public BuoyInfo getMyInfo(){
		return myInfo;
	}


	public int getPort() {
		return myInfo.getPort();
	}


	public void setPort(int port) {
		myInfo.setPort(port);
	}


	public String getHostName(){
		return myInfo.getHostName();
	}


	public void setHostName(String hostName){
		myInfo.setHostName(hostName);
	}

//	public String getCostValue(){
//		return costValue;
//	}

//	public void setCostValue(String costValue){
//		this.costValue = costValue;
//	}

}
