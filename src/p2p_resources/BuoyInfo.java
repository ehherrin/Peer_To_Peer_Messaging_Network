package p2p_resources;

public class BuoyInfo {
	// <ip_address>:<port_number>
	private String id;
	// <ip_address>
	private String host;
	// <port_number>
	private int port;
	// <buoy_name>
	private String hostName;

	public void setHostName(String hostName){
		this.hostName = hostName;
	}


	public String getHostName(){
		return hostName;
	}


	public BuoyInfo(String id, String host, int port, String hostName) {
		this.id = id;
		this.host = host;
		this.port = port;
		this.hostName = hostName;
	}


	public BuoyInfo(String id, String host, int port) {
		this(id, host, port, "");
	}


	public BuoyInfo(String host, int port) {
		this(host + ":" + port, host, port, "");
	}


	public BuoyInfo(String host, int port, String hostName) {
		this(host + ":" + port, host, port, hostName);
	}


	public String getHost() {
		return host;
	}


	public void setHost(String host) {
		this.host = host;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	// return (<ip_address>:<port_number>)
	public String toString() {
		return id + " (" + host + ":" + port + ")";
	}
}
