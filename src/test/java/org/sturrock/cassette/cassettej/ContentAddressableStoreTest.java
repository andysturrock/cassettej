package org.sturrock.cassette.cassettej;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class ContentAddressableStoreTest {

	protected static ContentAddressableStore cas;
	private String helloWorldString = "Hello World";
	// Precomputed sha1 hash of "Hello World"
	protected Hash helloWorldHash = new Hash("0A4D55A8D778E5022FAB701977C5D840BBC486D0");
	// Precomputed gzip byte array of "Hello World"
	private byte[] helloWorldEncodedBytes = { 31, -117, 8, 0, 0, 0, 0, 0, 0, 0, -13, 72, -51, -55, -55, 87, 8, -49, 47,
			-54, 73, 1, 0, 86, -79, 23, 74, 11, 0, 0, 0 };
	private String goodbyeWorldString = "Goodbye World";
	protected Hash goodbyeWorldHash = new Hash("D409B5D36068592A1C06C29FC3B7F16839398793");

	private Hash writeHelloWorld() throws IOException {
		return writeString(helloWorldString, new LinkedList<ContentEncoding>());
	}

	private Hash writeHelloWorld(ContentEncoding contentEncoding) throws IOException {
		List<ContentEncoding> contentEncodings = new LinkedList<ContentEncoding>();
		contentEncodings.add(contentEncoding);
		return writeString(helloWorldString, contentEncodings);
	}

	private Hash writeString(String string) throws IOException {
		return writeString(string, new LinkedList<ContentEncoding>());
	}

	private Hash writeString(String string, ContentEncoding contentEncoding) throws IOException {
		List<ContentEncoding> contentEncodings = new LinkedList<ContentEncoding>();
		contentEncodings.add(contentEncoding);
		return writeString(string, contentEncodings);
	}

	private Hash writeString(String string, List<ContentEncoding> contentEncodings) throws IOException {

		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);

		try (InputStream stream = new ByteArrayInputStream(bytes);) {
			Hash hash = cas.write(stream, contentEncodings);
			return hash;
		}
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
		assertEquals(helloWorldHash, actual);
	}

	@Test
	public void testEncodedWrite() throws IOException {
		Hash actual;
		actual = writeHelloWorld(new GZIPContentEncoding());
		assertEquals(helloWorldHash, actual);
	}

	@Test
	public void testContains() throws IOException {
		writeHelloWorld();
		// Now cas should contain some content with this hash
		assertEquals(true, cas.contains(helloWorldHash));

		// And shouldn't contain this hash
		assertEquals(false, cas.contains(goodbyeWorldHash));

		writeString(goodbyeWorldString);
		// And now it should
		assertEquals(true, cas.contains(goodbyeWorldHash));
	}

	@Test
	public void testEncodedContains() throws IOException {
		ContentEncoding encoding = new GZIPContentEncoding();
		writeHelloWorld(encoding);
		// Now cas should contain some content with this hash
		assertEquals(true, cas.contains(helloWorldHash, encoding));

		// And shouldn't contain this hash
		assertEquals(false, cas.contains(goodbyeWorldHash, encoding));

		writeString(goodbyeWorldString, encoding);
		// And now it should
		assertEquals(true, cas.contains(goodbyeWorldHash, encoding));
	}

	@Test
	public void testGetContentLength() throws IOException {
		writeHelloWorld();
		// The content length should be 11 (Hello World)
		assertEquals(helloWorldString.length(), cas.getContentLength(helloWorldHash));
	}

	@Test
	public void testEncodedGetContentLength() throws IOException {
		writeHelloWorld(new GZIPContentEncoding());
		// The content length should be 11 (Hello World)
		assertEquals(helloWorldString.length(), cas.getContentLength(helloWorldHash));
		// The encoded content length should be 48 (gzipped Hello World)
		assertEquals(helloWorldEncodedBytes.length, cas.getContentLength(helloWorldHash, new GZIPContentEncoding()));
	}

	@Test
	public void testGetHashes() throws IOException {
		
		List<Hash> hashes;
		hashes = cas.getHashes();
		// Should only be nothing in there
		assertEquals(0, hashes.size());
		
		writeHelloWorld();

		hashes = cas.getHashes();

		// Should only be one piece of content
		assertEquals(1, hashes.size());
		// The hash should be the same as above
		assertEquals(helloWorldHash, hashes.get(0));

		writeString(goodbyeWorldString);
		hashes = cas.getHashes();
		assertEquals(2, hashes.size());
	}

	@Test
	public void testRead() throws IOException {
		writeHelloWorld();

		try (InputStream stream = cas.read(helloWorldHash);) {
			if (stream == null)
				fail("Content not found");
			String content = new String(readFully(stream), StandardCharsets.UTF_8);
			// Content should be Hello World
			assertEquals(helloWorldString, content);
		}
	}

	@Test
	public void testEncodedRead() throws IOException {
		ContentEncoding encoding = new GZIPContentEncoding();
		writeHelloWorld(encoding);

		try (InputStream stream = cas.read(helloWorldHash, encoding);) {
			if (stream == null)
				fail("Content not found");

			try (InputStream decodedStream = encoding.decode(stream);) {
				String content = new String(readFully(decodedStream), StandardCharsets.UTF_8);
				// Content should be Hello World
				assertEquals(helloWorldString, content);
			}
		}
	}

	@Test
	public void testDelete() throws IOException {
		writeHelloWorld(new GZIPContentEncoding());
		writeString(goodbyeWorldString);
		List<Hash> hashes;
		hashes = cas.getHashes();
		// Should two pieces of content
		assertEquals(2, hashes.size());

		Hash hash = hashes.get(0);
		cas.delete(hash);
		hashes = cas.getHashes();
		// Now should be only one piece of content
		assertEquals(1, hashes.size());
		
		hashes = cas.getHashes();
		hash = hashes.get(0);
		cas.delete(hash);
		hashes = cas.getHashes();
		// Now should be no content
		assertEquals(0, hashes.size());
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

	@Test
	public void testContentAddedListener() throws IOException {
		ContentAddressableStoreListenerTestAdded contentAddressableStoreListenerTest = new ContentAddressableStoreListenerTestAdded();
		cas.addListener(contentAddressableStoreListenerTest);

		writeHelloWorld();

		ContentAddressableStoreEvent event = contentAddressableStoreListenerTest.getEvent();
		Hash expectedHash = helloWorldHash;
		Hash actualHash = event.getHash();
		assertEquals(expectedHash, actualHash);

		ContentAddressableStore expectedSource = cas;
		ContentAddressableStore actualSource = (ContentAddressableStore) event.getSource();
		assertEquals(expectedSource, actualSource);

		// Must remove the listener as don't want it firing in other tests.
		cas.removeListener(contentAddressableStoreListenerTest);
	}

	@Test
	public void testContentRemovedListener() throws IOException {
		ContentAddressableStoreListenerTestRemoved contentAddressableStoreListenerTest = new ContentAddressableStoreListenerTestRemoved();
		writeHelloWorld();
		cas.addListener(contentAddressableStoreListenerTest);
		cas.delete(helloWorldHash);

		ContentAddressableStoreEvent event = contentAddressableStoreListenerTest.getEvent();
		Hash expectedHash = helloWorldHash;
		Hash actualHash = event.getHash();
		assertEquals(expectedHash, actualHash);

		ContentAddressableStore expectedSource = cas;
		ContentAddressableStore actualSource = (ContentAddressableStore) event.getSource();
		assertEquals(expectedSource, actualSource);

		// Must remove the listener as don't want it firing in other tests.
		cas.removeListener(contentAddressableStoreListenerTest);
	}
}
