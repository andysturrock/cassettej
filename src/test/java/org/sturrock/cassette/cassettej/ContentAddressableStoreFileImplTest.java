package org.sturrock.cassette.cassettej;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
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
		fileCas = createCas();
		cas = fileCas;
	}

	@After
	public void tearDown() throws IOException {
		deleteTempDirectory();
	}
	
	private ContentAddressableStoreFileImpl createCas() throws IOException {
		Properties properties = new Properties();
		return createCas(properties);
	}
	
	private ContentAddressableStoreFileImpl createCas(Properties properties) throws IOException {
		properties.put(ContentAddressableStoreFileImpl.rootPathPropertyName, tempDir.toString());
		return new ContentAddressableStoreFileImpl(properties);
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
	
	private class CasWriter implements Runnable {

		private ContentAddressableStoreFileImpl cas;
		private String content;
		private IOException e;
		
		public CasWriter(ContentAddressableStoreFileImpl cas, String content) {
			this.cas = cas;
			this.content = content;
		}
		@Override
		public void run() {
			try {
				for(int i = 0; i < 1000; ++i) {
					InputStream inputStream = IOUtils.toInputStream(content, "UTF-8");
					cas.write(inputStream);
					inputStream.close();
				}
			} catch (IOException e) {
				this.e = e;
			}
		}
		public IOException getException() {
			return e;
		}
	}
	
	@Test
	public void testConcurrentWrites() throws IOException, InterruptedException {
		// This isn't massively reliable but run enough times you will get
		// a java.nio.file.FileAlreadyExistsException if the Files.move() call 
		// (roughly line 153 at time of writing) in ContentAddressableFileStoreImpl
		// doesn't use StandardCopyOption.REPLACE_EXISTING
		// A reliable way to trigger the exception is to add a Thread.sleep(1000) just
		// before the Files.move() call. 
		ContentAddressableStoreFileImpl fileCas2 = createCas();
		String source = "This is the source of my input stream";
		CasWriter casWriter1 = new CasWriter(fileCas, source);
		CasWriter casWriter2 = new CasWriter(fileCas2, source);
		Thread thread1 = new Thread(casWriter1);
		Thread thread2 = new Thread(casWriter2);
		thread1.start();
		thread2.start();
		thread1.join();
		thread2.join();
		IOException e = casWriter1.getException(); 
		if(e != null) {
			throw e;
		}
		e = casWriter2.getException(); 
		if(e != null) {
			throw e;
		}
	}
	
	@Test
	public void testSetAtomicMoveProperty() throws IOException {
		// Default is false
		assertEquals(false, fileCas.isUsingAtomicMove());
		
		// Create one with true
		Properties properties = new Properties();
		properties.put(ContentAddressableStoreFileImpl.atomicMovePropertyName, "true");
		ContentAddressableStoreFileImpl fileCas2 = createCas(properties);
		assertEquals(true, fileCas2.isUsingAtomicMove());
	}
}
