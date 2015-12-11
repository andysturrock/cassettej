package org.sturrock.cassette.cassettej;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple ContentAddressableStoreFileImpl.
 */
public class ContentAddressableStoreFileImplTest extends ContentAddressableStoreTest {
	private Path tempDir;
	private ContentAddressableStoreFileImpl fileCas;

	@Before
	public void setUp() throws IOException {
		tempDir = Files.createTempDirectory("ContentAddressableStoreFileImplTest");
		Properties properties = new Properties();
		properties.put(ContentAddressableStoreFileImpl.rootPathPropertyName, tempDir.toString());
		fileCas = new ContentAddressableStoreFileImpl(properties);
		cas = fileCas;
	}

	@After
	public void tearDown() throws IOException {
		deleteTempDirectory();
	}

	private void deleteTempDirectory() throws IOException {
		Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
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

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor() throws IOException {
		Properties properties = new Properties();
		cas = new ContentAddressableStoreFileImpl(properties);
	}

	@Test // Separate test to check all the files are deleted
	public void testDelete() throws IOException {
		super.testDelete();

		String helloString = helloWorldHash.getString();

		Path subPath = fileCas.getSubPath(helloString);
		// Directory should be gone now
		if (Files.exists(subPath)) {
			fail("Directory " + subPath + " should have been deleted.");
		}
	}

	@Test
	public void testGetSubPath() {
		String hashString = "1234567890";
		Path subPath = fileCas.getSubPath(hashString);
		Path rootPath = fileCas.getRootPath();
		Path expected = rootPath.resolve(hashString.substring(0, fileCas.hashPrefixLength));
		assertEquals(expected, subPath);
	}

	@Test
	public void testGetContentPath() {
		String hashString = "1234567890";
		Path contentPath = fileCas.getContentPath(hashString);
		Path rootPath = fileCas.getRootPath();
		Path expected = rootPath.resolve(hashString.substring(0, fileCas.hashPrefixLength));
		expected = expected.resolve(hashString.substring(fileCas.hashPrefixLength));
		assertEquals(expected, contentPath);
	}
}
