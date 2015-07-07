package org.sturrock.cassette.cassettej;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple ContentAddressableStoreFileImpl.
 */
public class ContentAddressableStoreFileImplTest {
	private ContentAddressableStore cas;
	private Path tempDir;
	private String helloWorldString = "Hello World";
	// Precomputed sha1 hash of "Hello World"
	private byte[] helloWorldHash = Hash
			.getBytes("0A4D55A8D778E5022FAB701977C5D840BBC486D0");

	@Before
	public void setUp() throws IOException {
		tempDir = Files.createTempDirectory("ContentAddressableStoreFileImplTest");
		Properties properties = new Properties();
		properties.put(ContentAddressableStoreFileImpl.rootPathPropertyName,
				tempDir.toString());
		cas = new ContentAddressableStoreFileImpl(properties);
	}

	@After
	public void tearDown() throws IOException {
		deleteTempDirectory();
	}

	private void deleteTempDirectory() throws IOException {
		Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException e)
					throws IOException {
				if (e == null) {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else {
					// directory iteration failed
					throw e;
				}
			}
		});
	}

	private byte[] writeHelloWorld() throws IOException {

		byte[] bytes = helloWorldString.getBytes(StandardCharsets.UTF_8);

		InputStream stream = new ByteArrayInputStream(bytes);
		byte[] hash = cas.write(stream);
		stream.close();
		return hash;
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor() throws IOException {
		Properties properties = new Properties();
		cas = new ContentAddressableStoreFileImpl(properties);
	}

	@Test
	public void testWrite() throws IOException {
		byte[] actual;
		actual = writeHelloWorld();
		assertArrayEquals(actual, helloWorldHash);
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
