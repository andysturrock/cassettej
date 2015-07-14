package org.sturrock.cassette.cassettej;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class ContentAddressableStoreTest {

	protected static ContentAddressableStore cas;
	private String helloWorldString = "Hello World";
	// Precomputed sha1 hash of "Hello World"
	private Hash helloWorldHash = new Hash("0A4D55A8D778E5022FAB701977C5D840BBC486D0");

	private Hash writeHelloWorld() throws IOException {

		byte[] bytes = helloWorldString.getBytes(StandardCharsets.UTF_8);

		InputStream stream = new ByteArrayInputStream(bytes);
		Hash hash = cas.write(stream);
		stream.close();
		return hash;
	}
	
	@Before
	public void setUp() throws IOException {
	}
	
	@After
	public void tearDown() throws IOException {
		
	}
	
	@Test
	public void testWrite() throws IOException {
		Hash actual;
		actual = writeHelloWorld();
		assertEquals(actual, helloWorldHash);
	}
	
	@Test
	public void testContains() throws IOException {
		writeHelloWorld();
		// Now cas should contain some content with this hash
		assert (cas.contains(helloWorldHash));
	}
	
	@Test
	public void testGetContentLength() throws IOException {
		writeHelloWorld();
		// The content length should be 11 (Hello World)
		assertEquals(cas.getContentLength(helloWorldHash),
				helloWorldString.length());
	}
	
	@Test
	public void testGetHashes() throws IOException {
		writeHelloWorld();

		List<Hash> hashes;
		hashes = cas.getHashes();
		// Should only be one piece of content
		assertEquals(hashes.size(), 1);
		// The hash should be the same as above
		assertEquals(helloWorldHash, hashes.get(0));

	}
	
	@Test
	public void testRead() throws IOException {
		writeHelloWorld();

		try (InputStream stream = cas.read(helloWorldHash);) {
			if (stream == null)
				fail("Content not found");
			String content = new String(readFully(stream),
					StandardCharsets.UTF_8);
			// Content should be Hello World
			assertEquals(helloWorldString, content);
		}
	}
	
	@Test
	public void testDelete() throws IOException {
		writeHelloWorld();
		List<Hash> hashes;
		hashes = cas.getHashes();
		// Should only be one piece of content
		assertEquals(hashes.size(), 1);

		Hash hash = hashes.get(0);

		cas.delete(hash);

		hashes = cas.getHashes();
		// Now should be no content
		assertEquals(hashes.size(), 0);
	}
	
	private static byte[] readFully(InputStream inputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length = 0;
		while ((length = inputStream.read(buffer)) != -1) {
			baos.write(buffer, 0, length);
		}
		return baos.toByteArray();
	}
}
