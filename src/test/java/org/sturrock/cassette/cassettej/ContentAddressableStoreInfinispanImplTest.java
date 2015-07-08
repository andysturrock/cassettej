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
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ContentAddressableStoreInfinispanImplTest {
	
	private String helloWorldString = "Hello World";
	// Precomputed sha1 hash of "Hello World"
	private byte[] helloWorldHash = Hash
			.getBytes("0A4D55A8D778E5022FAB701977C5D840BBC486D0");
	
	private ContentAddressableStoreInfinispanImpl cas;
	
	private byte[] writeHelloWorld() throws IOException {

		byte[] bytes = helloWorldString.getBytes(StandardCharsets.UTF_8);

		InputStream stream = new ByteArrayInputStream(bytes);
		byte[] hash = cas.write(stream);
		stream.close();
		return hash;
	}
	
	@Before
	public void setUp() throws IOException {
		Properties properties = new Properties();
		properties.put(ContentAddressableStoreInfinispanImpl.configFilePropertyName, "infinispan.xml");
		properties.put(ContentAddressableStoreInfinispanImpl.cacheNamePropertyName, "CassetteJ");
		cas = new ContentAddressableStoreInfinispanImpl(properties);
	}
	
	@After
	public void tearDown() {
		
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructor() throws IOException {
		Properties properties = new Properties();
		new ContentAddressableStoreInfinispanImpl(properties);
	}
	
	@Test(expected = IOException.class)
	public void testConstructor2() throws IOException {
		Properties properties = new Properties();
		properties.put(ContentAddressableStoreInfinispanImpl.configFilePropertyName, "does_not_exist");
		new ContentAddressableStoreInfinispanImpl(properties);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructor3() throws IOException {
		Properties properties = new Properties();
		properties.put(ContentAddressableStoreInfinispanImpl.configFilePropertyName, "infinispan.xml");
		new ContentAddressableStoreInfinispanImpl(properties);
	}
	
	@Test
	public void testWrite() throws IOException {
		byte[] actual;
		actual = writeHelloWorld();
		assertArrayEquals(actual, helloWorldHash);
		
		System.out.println("actual = " + actual);
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

		List<byte[]> hashes;
		hashes = cas.getHashes();
		// Should only be one piece of content
		assertEquals(hashes.size(), 1);
		// The hash should be the same as above
		assertArrayEquals(helloWorldHash, hashes.get(0));

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
		List<byte[]> hashes;
		hashes = cas.getHashes();
		// Should only be one piece of content
		assertEquals(hashes.size(), 1);

		byte[] hash = hashes.get(0);

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
