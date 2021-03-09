import p2p_resources.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;


public class Peer extends BuoyNode {
    public static final String INSERTPEER = "JOIN";
    public static final String LISTPEER = "LIST";
    public static final String PEERNAME = "NAME";
    public static final String PEERQUIT = "QUIT";
    public static final String HOSTNAME = "HNAM";
    public static final String REPLY = "REPL";
    public static final String FILEXFR = "FXFR";


    public Peer(int maxPeers, BuoyInfo my_info) {
        super(maxPeers, my_info);
        this.addHandler(INSERTPEER, new JoinHandler(this));
        this.addHandler(LISTPEER, new ListHandler(this));
        this.addHandler(PEERNAME, new NameHandler(this));
        this.addHandler(HOSTNAME, new HNamHandler(this));
        this.addHandler(PEERQUIT, new QuitHandler(this));
        this.addHandler(FILEXFR, new FileXferHandler(this));
    }


    public void updatePortNumber(){
        this.setPort(this.getMyInfo().getPort());
    }


    public List<BuoyPeerMessage> sendListPeerRequestToNeighbor(BuoyInfo neighbor_info_object){
        return this.connectAndSend(neighbor_info_object, LISTPEER, "", true);
    }


    public void obtainPeerIdFromNeighbor(BuoyInfo neighbor_info_object)
            throws IndexOutOfBoundsException{
        List<BuoyPeerMessage> peer_name_response_list
                = this.connectAndSend(neighbor_info_object, PEERNAME, "", true);
        String peer_id = peer_name_response_list.get(0).getMsgData();
        neighbor_info_object.setId(peer_id);
    }


    public void obtainHostNameRequestFromNeighbor(BuoyInfo neighbor_info_object)
            throws IndexOutOfBoundsException{
        List<BuoyPeerMessage> host_name_response_list
                = this.connectAndSend(neighbor_info_object, HOSTNAME, "", true);
        String hostName = host_name_response_list.get(0).getMsgData();
        neighbor_info_object.setHostName(hostName);
    }


    public void sendInsertPeerRequestToNeighbor(BuoyInfo neighbor_info_object)
            throws IndexOutOfBoundsException{
        String message_data = String.format("%s %s %d %s", this.getId(), this.getHost(),
                this.getPort(), this.getHostName());
        String insert_peer_response_string
                = this.connectAndSend(neighbor_info_object, INSERTPEER, message_data, true).get(0).getMsgType();
        if (!insert_peer_response_string.equals(REPLY) || this.getPeerKeys().contains(neighbor_info_object.getId())){
            throw new IndexOutOfBoundsException();
        }
        this.addPeer(neighbor_info_object);
    }


    public void determineNextObservedNeighbor(BuoyInfo neighbor_info_object, int remaining_hop_count,
                                              String execution_mode){
        List<BuoyPeerMessage> peer_names_response_list = sendListPeerRequestToNeighbor(neighbor_info_object);
        if (peer_names_response_list.size() > 1) {
            // Remove the message type data.
            peer_names_response_list.remove(0);
            for (BuoyPeerMessage pm : peer_names_response_list) {
                // Gather next peer ID, host name, and port number from the list of gathered PeerMessage data
                String[] data = pm.getMsgData().split("\\s");
                String next_peer_id = data[0];
                String next_host = data[1];
                int next_port = Integer.parseInt(data[2]);
                BuoyInfo next_buoy_neighbor_info_object = new BuoyInfo(next_host, next_port);
                // If the next peer in the list of peers isn't the current Buoy (this avoids looping forever),
                // repeat the above process for the next Buoy.
                if (!next_peer_id.equals(this.getId())) {
                    findMyLocalBuoyPeers(next_buoy_neighbor_info_object, remaining_hop_count - 1,
                            execution_mode);
                }
            }
        }
    }


    public void findMyLocalBuoyPeers(BuoyInfo neighbor_info_object, int remaining_hop_count, String execution_mode) {
        updatePortNumber();
        // Recursion will stop here when the maximum number of peers or hops for the current Buoy has been reached.
        // Also, recursion will stop if any information gathering goes wrong.
        if (this.maxPeersReached() || remaining_hop_count <= 0){return;}

        try {
            // Attempt to get and store the "ip:port" peer name from the currently observed neighbor.
            obtainPeerIdFromNeighbor(neighbor_info_object);
            // Attempt to get and store the hostname of the currently observed neighbor.
            obtainHostNameRequestFromNeighbor(neighbor_info_object);
            // Attempt to add this buoy's information to the peer list of the currently observed neighbor.
            sendInsertPeerRequestToNeighbor(neighbor_info_object);
        }catch (IndexOutOfBoundsException e){
            return;
        }
        // Determine if there are more neighbors to left explore.
        determineNextObservedNeighbor(neighbor_info_object, remaining_hop_count, execution_mode);
    }


    public String sendFileAndRequestAck(BuoyInfo recipient, BuoyInfo target_info, byte[] file_bytes)
            throws IndexOutOfBoundsException{
        return this.connectAndSendFile(recipient, target_info, FILEXFR,
                file_bytes, true).get(0).getMsgData();
    }


    public void findAndSendToTarget(BuoyInfo server_info, BuoyInfo target_info,
                                    byte[] file_bytes, int remaining_attempts) {
        List<BuoyPeerMessage> server_names_response_list = sendListPeerRequestToNeighbor(server_info);
        if (remaining_attempts == 0){
            System.err.println("Client failed to process file.");
        }
        if (server_names_response_list.size() > 1) {
            // Remove the message type data.
            server_names_response_list.remove(0);
            for (BuoyPeerMessage pm : server_names_response_list) {
                // Gather next peer ID, host name, and port number from the list of gathered PeerMessage data
                String[] data = pm.getMsgData().split("\\s");
                String server_client_id = data[0];
                String server_client_host = data[1];
                int server_client_port = Integer.parseInt(data[2]);
                BuoyInfo server_client_info = new BuoyInfo(server_client_host, server_client_port);
                // If the next peer in the list of peers isn't the current Buoy (this avoids looping forever),
                // repeat the above process for the next Buoy.
                if (target_info.getId().equals(server_client_info.getId())) {
                    try {
                        String ack = sendFileAndRequestAck(server_info, target_info, file_bytes);
                        switch (ack) {
                            case "corrupted" -> findAndSendToTarget(server_info, target_info,
                                    file_bytes, remaining_attempts - 1);
                            case "ACK: file received" -> System.out.println("File share complete.");
                            case "failed" -> System.err.println("Server could not contact target.");
                        }
                    }catch (IndexOutOfBoundsException e){
                        System.err.println("Could not contact server.");
                    }
                }
            }
        }
    }


    private static class JoinHandler implements HandlerInterface {
        private final BuoyNode peer;

        public JoinHandler(BuoyNode peer) { this.peer = peer; }

        public void handleMessage(BuoyConnection buoyLink, BuoyPeerMessage msg) {
            if (peer.maxPeersReached()) {
                System.err.println("Max Peers Reached!");
                return;
            }
            String[] data = msg.getMsgData().split("\\s");
            if (data.length != 4) {
                System.err.println("Data length is incorrect!");
                return;
            }
            BuoyInfo info = new BuoyInfo(data[0], data[1], Integer.parseInt(data[2]));
            info.setHostName(data[3]);
            if (peer.getPeer(info.getId()) == null && !info.getId().equals(peer.getId())){
                peer.addPeer(info);
                buoyLink.sendData(new BuoyPeerMessage(REPLY, "Join: peer added: " + info.getId()));
            }
        }
    }


    private static class FileXferHandler implements HandlerInterface{
        private final Peer peer;

        public FileXferHandler(Peer peer) { this.peer = peer; }

        public void handleMessage(BuoyConnection buoyLink, BuoyPeerMessage msg) {
            if (!msg.getDestination().equals(peer.getMyInfo())){
                try {
                    String ack = peer.sendFileAndRequestAck(msg.getDestination(), msg.getDestination(),
                            msg.getMsgDataBytes());
                    if (ack.equals("ACK: file received")){
                        buoyLink.sendData(new BuoyPeerMessage(REPLY, "ACK: file received"));
                    }
                }catch (IndexOutOfBoundsException e){
                    buoyLink.sendData(new BuoyPeerMessage(REPLY, "failed"));
                }
            }else{
                try {
                    Date time_stamp = new Date();
                    String file_name = "media_file_via_" + msg.getClass()+ "_" + time_stamp.toInstant() + ".bin";
                    FileOutputStream file_out_stream
                            = new FileOutputStream(System.getProperty("user.home") + "/Downloads/" + file_name);
                    file_out_stream.write(msg.getMsgDataBytes());
                    buoyLink.sendData(new BuoyPeerMessage(REPLY, "ACK: file received"));
                }catch (IOException e){
                    buoyLink.sendData(new BuoyPeerMessage(REPLY, "corrupted"));
                    System.err.println(e.toString());
                }
            }

        }
    }


    private static class ListHandler implements HandlerInterface {
        private final BuoyNode peer;
        public ListHandler(BuoyNode peer) { this.peer = peer; }
        public void handleMessage(BuoyConnection buoyLink, BuoyPeerMessage msg) {
            buoyLink.sendData(new BuoyPeerMessage(REPLY,
                    String.format("%d", peer.getNumberOfPeers())));
            for (String pid : peer.getPeerKeys()) {
                buoyLink.sendData(new BuoyPeerMessage(REPLY,
                        String.format("%s %s %d %s", pid, peer.getPeer(pid).getHost(),
                                peer.getPeer(pid).getPort(), peer.getPeer(pid).getHostName())));
            }
        }
    }


    private static class NameHandler implements HandlerInterface {
        private final BuoyNode peer;

        public NameHandler(BuoyNode peer) { this.peer = peer; }

        public void handleMessage(BuoyConnection buoyLink, BuoyPeerMessage msg) {
            buoyLink.sendData(new BuoyPeerMessage(REPLY, peer.getId()));
        }
    }


    private static class HNamHandler implements HandlerInterface{
        private final BuoyNode peer;

        public HNamHandler(BuoyNode peer) { this.peer = peer; }

        public void handleMessage(BuoyConnection buoyLink, BuoyPeerMessage msg) {
            buoyLink.sendData(new BuoyPeerMessage(REPLY, peer.getHostName()));
        }
    }


    private static class QuitHandler implements HandlerInterface {
        private final BuoyNode peer;

        public QuitHandler(BuoyNode peer) { this.peer = peer; }

        public void handleMessage(BuoyConnection buoyLink, BuoyPeerMessage msg) {
            String pid = msg.getMsgData().trim();
            if (peer.getPeer(pid) != null) {
                peer.removePeer(pid);
                buoyLink.sendData(new BuoyPeerMessage(REPLY, "Quit: peer removed: " + pid));
            }
        }
    }

}
