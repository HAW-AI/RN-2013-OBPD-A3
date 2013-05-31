import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientReceiver extends Thread {

	private final DatagramSocket socket;
	
	private BlockingQueue<Integer> queue=new LinkedBlockingQueue<Integer>();

	public ClientReceiver(DatagramSocket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			byte[] receiveData = new byte[8];
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			try {
				socket.receive(receivePacket);
				final byte[] data = receivePacket.getData();	
				queue.add(new BigInteger(data).intValue());
			} catch (IOException e) {
				if (!isInterrupted())
					System.err.println("Error receiving packet: " + e.getMessage());
			}

		}
	}
	
	public BlockingQueue<Integer> getQueue() {
		return queue;
	}

}
