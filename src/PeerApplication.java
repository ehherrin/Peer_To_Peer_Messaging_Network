import p2p_resources.BuoyInfo;
import p2p_resources.SimplePingStabilizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PeerApplication extends JFrame{
    private static final int FRAME_WIDTH = 750, FRAME_HEIGHT = 500, MAX_HOPS = 1, DEFAULT_PORT_NO = 0, MAX_ATTEMPTS = 3;
    private static final String DEFAULT_IP = "127.0.0.1", DEFAULT_HOSTNAME = "CLIENT_" + DEFAULT_PORT_NO;
    private static final String[] SERVER_CONFIG_FILE_LOCATION = new String[]{"src\\server_config.txt"};
    private JPanel peersPanel = new JPanel();
    private DefaultListModel<String> peersModel;

    private PeerApplication(int max_peers, ArrayList<BuoyInfo> buoyPeersInfo, String execution_mode){
        this(max_peers, buoyPeersInfo, execution_mode, new byte[]{});
    }

    private PeerApplication(int max_peers, ArrayList<BuoyInfo> buoyPeersInfo, String execution_mode, byte[] file_bytes)
    {
        switch (execution_mode) {
            case "buoyapp" -> {
                Peer my_peer = new Peer(max_peers, buoyPeersInfo.get(0));
                (new Thread(my_peer::mainLoop)).start();
                List<BuoyInfo> buoyNeighbors = buoyPeersInfo.subList(2, buoyPeersInfo.size());
                for (BuoyInfo neighbor_info : buoyNeighbors) {
                    my_peer.findMyLocalBuoyPeers(neighbor_info, MAX_HOPS, execution_mode);
                }
                my_peer.startStabilizer(new SimplePingStabilizer(my_peer), 5000);
                setupFrame(my_peer);
                new Timer(5000, new RefreshListener(my_peer)).start();
            }
            case "server" -> {
                BuoyInfo server_info = buoyPeersInfo.get(0);
                Peer my_server = new Peer(max_peers, server_info);
                (new Thread(my_server::mainLoop)).start();
//                my_server.findMyLocalBuoyPeers(server_info, MAX_HOPS, execution_mode);
                my_server.startStabilizer(new SimplePingStabilizer(my_server), 5000);
                setupFrame(my_server);
                new Timer(1000, new RefreshListener(my_server)).start();
            }
            case "client" -> {
                BuoyInfo client_info = buoyPeersInfo.get(0);
                BuoyInfo server_info = buoyPeersInfo.get(1);
                Peer my_client = new Peer(max_peers, client_info);
                // Get client socketed and ready to handle communications
                (new Thread(my_client::mainLoop)).start();
                my_client.setPort(my_client.getMyInfo().getPort());
                // Repurposed Buoy method that will share client data with the server.
                // This will allow the server to keep a list of all reachable hosts.
                my_client.findMyLocalBuoyPeers(server_info, MAX_HOPS, execution_mode);
                if (buoyPeersInfo.size() > 2 && file_bytes.length > 0) {
                    BuoyInfo target_info = buoyPeersInfo.get(2);
                    my_client.findAndSendToTarget(server_info, target_info, file_bytes, MAX_ATTEMPTS);
                }
                my_client.startStabilizer(new SimplePingStabilizer(my_client), 5000);
                setupFrame(my_client);
                new Timer(1000, new RefreshListener(my_client)).start();
            }
        }
    }

// *************************************GUI Classes and Methods********************************************
    private void setupFrame(Peer node_entity)
    {
        JFrame frame = new JFrame(node_entity.getHostName() + " @ " + node_entity.getId());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        peersPanel.setLayout(new GridLayout(2, 3));
        peersPanel.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        peersModel = new DefaultListModel<>();
        JList<String> peersList = new JList<>(peersModel);
        peersPanel = initPanel(new JLabel("Client  |  Last Heard"), peersList);
        frame.add(peersPanel, BorderLayout.NORTH);
        frame.setVisible(true);

    }


    private JPanel initPanel(JLabel textField, JList<String> list){
        JPanel panel = new JPanel();
        panel.add(textField);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        panel.add(scrollPane);
        return panel;
    }


    private void updatePeerList(Peer node_entity){
        peersModel.removeAllElements();
        Date date = new Date();
        for (String pid : node_entity.getPeerKeys()) {
            peersModel.addElement("    " + node_entity.getPeer(pid).getHostName() + "  |  " + date);
        }
    }


    class RefreshListener implements ActionListener {
        Peer node_entity;
        private RefreshListener(Peer node_entity){
            this.node_entity = node_entity;
        }
        public void actionPerformed(ActionEvent e) {
            updatePeerList(node_entity);
        }
    }
    // *************************************GUI Classes and Methods********************************************


    private static ArrayList<String> obtainConfigDataArray(String[] args){
        String configFilePath = args[0];
        String data;
        String line;
        String[] dataArray;
        ArrayList<String> configDataArray = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
            while ((line = reader.readLine()) != null) {
                dataArray = line.split(":\\s+");
                if (dataArray.length == 2) {
                    data = line.split(":\\s+")[1];
                    configDataArray.add(data);
                }
            }
        } catch (IOException ignored) {}
        return configDataArray;
    }

    private static Object[] obtainServerConfigData(){
        ArrayList<String> configDataArray = obtainConfigDataArray(SERVER_CONFIG_FILE_LOCATION);
        String my_server_name = configDataArray.get(0);
        String my_server_ip = configDataArray.get(1).split(":")[0];
        int my_server_port = Integer.parseInt(configDataArray.get(1).split(":")[1]);
        return new Object[]{my_server_ip, my_server_port, my_server_name};
    }

    public static byte[] convertBinFileToByteArray(String bin_file_path) throws IOException {
        File file = new File(bin_file_path);
        InputStream bin_file_input_stream = new FileInputStream(file);
        byte[] file_bytes = new byte[(int)file.length()];
        bin_file_input_stream.read(file_bytes);
        bin_file_input_stream.close();
        return file_bytes;
    }



    public static void main(String[] args) {
        ArrayList<String> configDataArray;
        try{
            String execution_mode = args[0];
            execution_mode = execution_mode.toLowerCase();
            switch (execution_mode) {
                case "server" -> {
                    ArrayList<BuoyInfo> server_info = new ArrayList<>();
                    Object[] server_config_data = obtainServerConfigData();
                    // Server configuration data.
                    server_info.add(new BuoyInfo((String)server_config_data[0],
                            (Integer)server_config_data[1], (String)server_config_data[2]));
                    new PeerApplication(Integer.MAX_VALUE, server_info, execution_mode);
                }
                case "client" -> {
                    ArrayList<BuoyInfo> buoy_client_server_and_target_info = new ArrayList<>();
                    byte[] file_bytes;
                    Object[] server_config_data = obtainServerConfigData();
                    // Client configuration data.
                    buoy_client_server_and_target_info.add(new BuoyInfo(DEFAULT_IP, DEFAULT_PORT_NO, DEFAULT_HOSTNAME));
                    // Server configuration data.
                    buoy_client_server_and_target_info.add(new BuoyInfo((String)server_config_data[0],
                            (Integer)server_config_data[1], (String)server_config_data[2]));
                    if (args.length == 3){
                        file_bytes = convertBinFileToByteArray(args[2]);
                        String target_ip = args[1].split(":")[0];
                        int target_port = Integer.parseInt(args[1].split(":")[1]);
                        buoy_client_server_and_target_info.add(new BuoyInfo(target_ip, target_port));
                    }else{
                        file_bytes = new byte[]{};
                    }
                    new PeerApplication(Integer.MAX_VALUE, buoy_client_server_and_target_info, execution_mode, file_bytes);
                }
                case "buoyapp" -> {
                    ArrayList<BuoyInfo> buoy_peer_and_self_info = new ArrayList<>();
                    configDataArray = obtainConfigDataArray(args);
                    String buoy_host_name = configDataArray.get(0);
                    String buoyAddress = configDataArray.get(1);
                    String my_buoy_ip = buoyAddress.split(":")[0];
                    int my_buoy_port = Integer.parseInt(buoyAddress.split(":")[1]);
                    List<String> buoyNeighbors = configDataArray.subList(1, configDataArray.size());
                    buoy_peer_and_self_info.add(new BuoyInfo(my_buoy_ip, my_buoy_port, buoy_host_name));
                    for (String buoNeighbor : buoyNeighbors) {
                        String peer_ip = buoNeighbor.split(":")[0];
                        int peer_port = Integer.parseInt(buoNeighbor.split(":")[1]);
                        buoy_peer_and_self_info.add(new BuoyInfo(peer_ip, peer_port));
                    }
                    new PeerApplication(Integer.MAX_VALUE, buoy_peer_and_self_info, execution_mode);
                }
            }
        }catch (Exception e){
            System.err.println("Usage Options:");
            System.err.println("   A)\tjava server");
            System.err.println("   B)\tjava client");
            System.err.println("   B)\tjava client <target_ip_address> <media_file_path>");
            System.err.println("   C)\tjava BuoyApp <buoy_config_file_path> ");
        }

    }
}
