import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.Logger;

public class ClientReceiver extends Thread {

	private final DatagramSocket socket;
	private final SendBuffer buffer;
	private final Logger LOG = Logger.getLogger(ClientReceiver.class.getName());

	public ClientReceiver(DatagramSocket socket, SendBuffer buffer) {
		this.socket = socket;
		this.buffer = buffer;
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
				new Thread() {
					public void run() {
						long timeReceived=System.nanoTime();
						buffer.receivedAck(new BigInteger(data).intValue(),timeReceived);
					};
				}.start();
			} catch (IOException e) {
				if (!isInterrupted())
					LOG.warning("Error receiving packet: " + e.getMessage());
			}

		}
	}

}
