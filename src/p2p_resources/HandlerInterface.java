package p2p_resources;

public interface HandlerInterface
{
    void handleMessage(BuoyConnection peerconn, BuoyPeerMessage msg);
}
