import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SendBuffer extends Thread {

	private final List<FCpacket> packetList;
	private final int windowSize;
	private final FileCopyClient fc;
	private int sendBase = 0;
	private final ClientReceiver receiver;

	private final BlockingQueue<FCpacket> queue = new LinkedBlockingQueue<FCpacket>();

	public SendBuffer(ClientReceiver receiver, List<FCpacket> packetList,
			int windowSize, InetAddress server, FileCopyClient fc) {
		this.packetList = packetList;
		this.windowSize = windowSize;
		this.fc = fc;
		this.receiver = receiver;
	}

	private void receivedAck(int i, long timeReceived) {
		fc.testOut("Received ack for :" + i + " (sendBase:" + sendBase + ")");
		if(i >= 0 && i < packetList.size()){
			FCpacket packet = packetList.get(i);
			packet.setValidACK(true);
			fc.cancelTimer(packet);
			long timestamp = packet.getTimestamp();
			fc.testOut("Timereceived:"+timeReceived +"; Timestamp:"+timestamp+";SeqNum:"+packet.getSeqNum());
			
			if (timestamp < timeReceived) {
				fc.computeTimeoutValue(timeReceived - timestamp);
			} else {
				fc.invalidRtts++;
			}
			if (sendBase == 0){
				sendBase=1;
				for(int n=1; n < packetList.size() && n< sendBase + windowSize;n++){
					addPacket(packetList.get(n));
				}
			}
			if (i == sendBase) {
				while (packetList.get(sendBase).isValidACK()
						&& sendBase + windowSize < packetList.size()) {
					sendBase += 1;
					addPacket(packetList.get(sendBase + windowSize - 1));
				}
			}
			if(isFinished()){
				byte[] finished="finished".getBytes();
				queue.add(new FCpacket(-1,finished,finished.length));
			}
		}
		else {
			fc.testOut("Ignored invalid ack: "+ i);
		}
	}

	void addPacket(long seqNum) {
		FCpacket packet = packetList.get((int) seqNum);
		addPacket(packet);
	}

	void addPacket(FCpacket packet) {
		if (!packet.isValidACK() && packet.getSeqNum() >= sendBase) {
			queue.add(packet);
		}
	}

	public boolean isFinished() {
		// I tried to check this every time an ACK is received and store it in a
		// variable, but then the main thread never ended.
		
		if (sendBase + windowSize >= packetList.size()
				|| windowSize > packetList.size()) {
			boolean allAck = true;
			for (int n = sendBase; n < packetList.size(); n++) {
				if (!packetList.get(n).isValidACK()) {
					fc.testOut("Failed at: " + n);
					allAck = false;
					break;
				}
			}
			fc.testOut("Finish check failed");
			return allAck;
		} else
			return false;
	}

	public BlockingQueue<FCpacket> getQueue() {
		return queue;
	}

	@Override
	public void run() {
		BlockingQueue<Integer> ackQueue = receiver.getQueue();
		try {
			while (true) {
				int i;
				i = ackQueue.take();
				receivedAck(i, System.nanoTime());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
