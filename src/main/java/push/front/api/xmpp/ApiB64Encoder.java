package push.front.api.xmpp;

import org.jivesoftware.smack.util.stringencoder.Base64;
public class ApiB64Encoder implements Base64.Encoder{

	@Override
	public byte[] decode(String input) {
		return java.util.Base64.getDecoder().decode(input);
	}

	@Override
	public byte[] decode(byte[] input, int offset, int len) {
		byte []dest = new byte[len];
		System.arraycopy(input, offset, dest, 0, len);
		return java.util.Base64.getDecoder().decode(dest);
	}

	@Override
	public String encodeToString(byte[] input, int offset, int len) {
		byte []dest = new byte[len];
		System.arraycopy(input, offset, dest, 0, len);
		return java.util.Base64.getEncoder().encodeToString(dest);
	}

	@Override
	public byte[] encode(byte[] input, int offset, int len) {
		byte []dest = new byte[len];
		System.arraycopy(input, offset, dest, 0, len);
		return java.util.Base64.getEncoder().encode(dest);
	}

}