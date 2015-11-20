package org.sturrock.cassette.cassettej;

/**
 * Factory to create concrete types of ContentEncoding
 * Currently only supports gzip
 *
 */
public class ContentEncodingFactory {

	public static ContentEncoding createEncoding(String encodingName) {
		// For now only support gzip
		if (encodingName.equals("gzip")) {
			return new GZIPContentEncoding();
		}
		return null;
	}
}
