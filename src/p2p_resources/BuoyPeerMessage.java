package p2p_resources;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class BuoyPeerMessage implements Serializable {

	private final String type;
	private final byte[] data;
	private BuoyInfo destination;

	public BuoyPeerMessage(String type, byte[] data, BuoyInfo destination) {
		this.type = type;
		this.data = data;
		this.destination = destination;
	}
	
	public BuoyPeerMessage(String type, byte[] data) {
		this(type, data, null);
	}
	
	
	public BuoyPeerMessage(String type, String data) {
		this(type, data.getBytes(), null);
	}


	public BuoyPeerMessage(BuoySocket s) throws IOException, ClassNotFoundException {
//		byte[] buoy_data = new byte[10000];
//		s.getBuoySocket().getInputStream().read(buoy_data);
//		ByteArrayInputStream message_stream = new ByteArrayInputStream(buoy_data);
		ObjectInputStream buoy_message_input_stream = new ObjectInputStream(s.getBuoySocket().getInputStream());
		BuoyPeerMessage streamed_buoy_message = (BuoyPeerMessage) buoy_message_input_stream.readObject();
		type = streamed_buoy_message.type;
		data = streamed_buoy_message.data;
	}
	
	
	public String getMsgType() {
		return new String(type);
	}


	public String getMsgData() {
		return new String(data);
	}


	public byte[] getMsgDataBytes(){ return data;}


	public BuoyInfo getDestination(){return destination;}

	
	public String toString() {
		return "PeerMessage[" + getMsgType() + ":" + getMsgData() + "]";
	}
}
