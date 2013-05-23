import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.logging.Logger;

public class SendBuffer {

	private final DatagramSocket socket;
	private final List<FCpacket> packetList;
	private final int windowSize;
	private final FileCopyClient fc;
	private int sendBase = 0;
	private final InetAddress server;
	private final Logger LOG = Logger.getLogger(SendBuffer.class.getName());

	public SendBuffer(DatagramSocket socket, List<FCpacket> packetList,
			int windowSize, InetAddress server, FileCopyClient fc) {
		this.socket = socket;
		this.packetList = packetList;
		this.windowSize = windowSize;
		this.fc = fc;
		this.server = server;
	}

	synchronized void receivedAck(int i, long timeReceived) {
		if (i >= 0 && i < packetList.size()) {
			LOG.info("Received ack for :" + i + " (sendBase:" + sendBase + ")");
			FCpacket packet = packetList.get(i);
			fc.cancelTimer(packet);
			if(packet.getTimestamp() < timeReceived){
				fc.computeTimeoutValue(timeReceived - packet.getTimestamp());
			}
			packet.setValidACK(true);
			if (i == sendBase) {
				while (packetList.get(sendBase).isValidACK()
						&& sendBase + windowSize < packetList.size()) {
					sendBase += 1;
					final int currentSendBase=sendBase;
					new Thread(){
						public void run() {
							sendPacket(packetList.get(currentSendBase + windowSize - 1));
						};
					}.start();
					
				}
			}
		}
	}

	synchronized void sendPacket(FCpacket packet) {
		DatagramPacket outgoing;
		outgoing = new DatagramPacket(packet.getSeqNumBytesAndData(),
				packet.getSeqNumBytesAndData().length, server,
				FileCopyServer.SERVER_PORT);
		LOG.info("Sending packet: " + packet.getSeqNum());
		packet.setTimestamp(System.nanoTime());
		fc.startTimer(packet);
		try {
			socket.send(outgoing);
		} catch (IOException e) {
			LOG.warning("Error sending packet: " + e.getMessage());
		}
	}

	void sendPacket(long seqNum) {
		if (seqNum >= 0 && seqNum < packetList.size()) {
			FCpacket packet = packetList.get((int) seqNum);
			if (!packet.isValidACK()) {
				sendPacket(packet);
			}
		}
	}

	void sendFirstWindow() {
		for (int i = 0; i < windowSize && i < packetList.size(); i++) {
			sendPacket(packetList.get(i));
		}
	}

	public boolean isFinished() {
		// I tried to check this every time an ACK is received and store it in a
		// variable, but then the main thread never ended.
		if (sendBase + windowSize == packetList.size()
				|| windowSize > packetList.size()) {
			boolean allAck = true;
			for (int n = sendBase; n < packetList.size(); n++) {
				if (!packetList.get(n).isValidACK()) {
					allAck = false;
					break;
				}
			}
			return allAck;
		} else
			return false;
	}

}
