import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FileCopyUtils {

	private static final Logger LOG = Logger.getLogger(FileCopyUtils.class
			.getName());

	private FileCopyUtils() {
	}

	static List<FCpacket> readFileToPackets(String path, int packetSize) {
		List<FCpacket> result = new ArrayList<FCpacket>();
		File file = new File(path);
		if (file.exists()) {
			try {
				RandomAccessFile f = new RandomAccessFile(path, "r");
				byte[] b = new byte[(int) f.length()];
				f.read(b);
				int payloadSize = packetSize - 8;
				for (int i = 0; i * payloadSize < f.length(); i++) {
					int individualPayloadSize=(i+1)*payloadSize > f.length() ? (int)(f.length()-i*payloadSize) : payloadSize;
					byte[] payload=new byte[individualPayloadSize];
					System.arraycopy(b, i*payloadSize,payload,0,individualPayloadSize);
					result.add(new FCpacket(i+1, payload, payload.length));
				}
				f.close();
			} catch (FileNotFoundException e) {
				LOG.warning("File not found : " + e.getMessage());
				return result;
			} catch (IOException e) {
				LOG.warning("Cannot read file : " + e.getMessage());
			}

		}
		return result;
	}
}
