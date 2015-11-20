package org.sturrock.cassette.cassettej;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;

/**
 * Defines an encoding for use with a ContentAddressableStore This allows data
 * to be stored in that format and retrieved using that format directly - ie the
 * work to compress/decompress is done once at storage time rather than multiple
 * times at retrieval time.
 */
public interface ContentEncoding {

	/**
	 * Get the name of the encoding - eg gzip, deflate
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * Encode a stream and return a new stream with this encoding type. Note
	 * that this method explicitly returns a DeflaterOutputStream. The caller
	 * can then call finish() on the stream. See
	 * {@link java.util.zip.DeflaterOutputStream#finish()} for more
	 * details.
	 * 
	 * @param stream
	 *            The unencoded stream
	 * @return The encoded stream
	 * @throws IOException
	 */
	public DeflaterOutputStream encode(OutputStream stream) throws IOException;

	/**
	 * Decode a stream with this encoding type and return a new stream.
	 * 
	 * @param stream
	 *            The encoded stream
	 * @return The decoded stream
	 * @throws IOException
	 */
	public InputStream decode(InputStream stream) throws IOException;
}
