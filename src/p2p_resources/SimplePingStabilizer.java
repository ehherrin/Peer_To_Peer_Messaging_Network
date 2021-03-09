package p2p_resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimplePingStabilizer implements StabilizerInterface {
	private final BuoyNode peer;
	private final String msgtype;
	
	public SimplePingStabilizer(BuoyNode peer) {
		this(peer, "PING");
	}
	
	public SimplePingStabilizer(BuoyNode peer, String msgtype) {
		this.peer = peer;
		this.msgtype = msgtype;
	}
	
	public void stabilizer() {
		List<String> to_delete = new ArrayList<>();
		// Tries to connect to all known peers in the BuoyNode "peers" hashtable. If it is unsuccessful,
		// then the peer will be removed from the buoy's hashtable.
		for (String peer_id : peer.getPeerKeys()) {
			boolean isconn = false;
			BuoyConnection peerconn = null;
			try {
				peerconn = new BuoyConnection(peer.getPeer(peer_id));
				peerconn.sendData(new BuoyPeerMessage(msgtype, ""));
				isconn = true;
			}
			catch (IOException e) {
				to_delete.add(peer_id);
			}
			if (isconn)
				peerconn.close();
		}
		for (String pid : to_delete) {
			peer.removePeer(pid);
		}
	}
}

