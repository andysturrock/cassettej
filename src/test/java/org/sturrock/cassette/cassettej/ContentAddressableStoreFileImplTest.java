package org.sturrock.cassette.cassettej;

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
public class ContentAddressableStoreFileImplTest extends
		ContentAddressableStoreTest {
	private Path tempDir;

	@Before
	public void setUp() throws IOException {
		tempDir = Files
				.createTempDirectory("ContentAddressableStoreFileImplTest");
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

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor() throws IOException {
		Properties properties = new Properties();
		cas = new ContentAddressableStoreFileImpl(properties);
	}

}
