package org.sturrock.cassette.cassettej;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

/*
 * Copyright 2015 Andy Sturrock
 * Derived from https://github.com/drewnoakes/cassette
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.zip.DeflaterOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * A content-addressable store backed by the file system. Default implementation
 * of ContentAddressableStore.
 * 
 */
public final class ContentAddressableStoreFileImpl extends ContentAddressableStoreImpl {
	/**
	 * The root path for all content within this store
	 */
	private final Path rootPath;

	/**
	 * 
	 * @return the root path for all content within this store
	 */
	public Path getRootPath() {
		return rootPath;
	}

	/**
	 * The size of the byte array buffer used for read/write operations.
	 */
	private final int bufferSize = 4096;

	/**
	 * The number of characters from the hash to use for the name of the top
	 * level subdirectories.
	 */
	public final int hashPrefixLength = 4;

	public final static String rootPathPropertyName = ContentAddressableStoreFileImpl.class.getName() + ".rootPath";

	/**
	 * Initialises the store, using rootPath as the root for all content.
	 * 
	 * @param rootPath
	 *            Root path for all content in this store.
	 * @throws IOException
	 */
	public ContentAddressableStoreFileImpl(Properties properties) throws IOException {
		if (properties == null)
			throw new IllegalArgumentException("properties");

		String rootPath = properties.getProperty(rootPathPropertyName);
		if (rootPath == null || rootPath.equals("")) {
			throw new IllegalArgumentException("No property " + rootPathPropertyName + " found");
		}
		this.rootPath = Paths.get(rootPath);

		if (!Files.isDirectory(this.rootPath))
			Files.createDirectories(this.rootPath);
	}

	@Override
	public Hash write(InputStream inputStream) throws IOException {
		return write(inputStream, new LinkedList<ContentEncoding>());
	}

	@Override
	public Hash write(InputStream inputStream, List<ContentEncoding> encodings) throws IOException {
		if (inputStream == null) {
			throw new IllegalArgumentException("inputStream");
		}
		if (encodings == null) {
			encodings = new LinkedList<ContentEncoding>();
		}

		Path tmpFile = Files.createTempFile("CassetteJ", ".tmp");
		try {
			Files.copy(inputStream, tmpFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			Files.delete(tmpFile);
			throw new IOException(e);
		}

		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}

		try (InputStream fileInputStream = new FileInputStream(tmpFile.toFile());) {
			int n = 0;
			byte[] buffer = new byte[bufferSize];
			while (n != -1) {
				n = fileInputStream.read(buffer);
				if (n > 0) {
					messageDigest.update(buffer, 0, n);
				}
			}
		}

		final byte[] bytes = messageDigest.digest();

		Hash hash = new Hash(bytes);

		// Determine the location for the content file
		Path contentPath = getContentPath(hash.getString());
		Path subPath = getSubPath(hash.getString());

		// Write the file if it doesn't already exist
		boolean contentAdded = false;
		if (!Files.exists(contentPath)) {
			// Ensure the sub-path exists
			if (!Files.isDirectory(subPath))
				Files.createDirectories(subPath);

			Files.move(tmpFile, contentPath);
			contentAdded = true;
		} else {
			Files.delete(tmpFile);
		}

		// Now write the encoded versions
		for (ContentEncoding encoding : encodings) {
			Path encodedContentPath = getContentPath(hash.getString() + "." + encoding.getName());
			if (Files.exists(encodedContentPath)) {
				continue;
			}
			try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(encodedContentPath));
					DeflaterOutputStream encodedOutputStream = encoding.encode(outputStream);
					InputStream rawContents = Files.newInputStream(contentPath);) {
				IOUtils.copy(rawContents, encodedOutputStream);
				encodedOutputStream.finish();
			}
		}

		// Only notify listeners after writing everything
		if (contentAdded) {
			notifyListenersContentAdded(hash);
		}

		// The caller receives the hash, regardless of whether the
		// file previously existed in the store
		return hash;
	}

	@Override
	public boolean contains(Hash hash) {
		if (hash == null)
			throw new IllegalArgumentException("hash");

		Path contentPath = getContentPath(hash.getString());
		return Files.exists(contentPath);
	}

	@Override
	public boolean contains(Hash hash, ContentEncoding contentEncoding) {
		if (hash == null) {
			throw new IllegalArgumentException("hash");
		}
		if (contentEncoding == null) {
			return contains(hash);
		}

		Path encodedContentPath = getContentPath(hash.getString() + "." + contentEncoding.getName());
		return Files.exists(encodedContentPath);
	}

	@Override
	public InputStream read(Hash hash, ContentEncoding contentEncoding) throws IOException {
		if (hash == null)
			throw new IllegalArgumentException("hash");
		// null contentEncoding means use no encoding
		if (contentEncoding == null) {
			return read(hash);
		}

		Path encodedContentPath = getContentPath(hash.getString() + "." + contentEncoding.getName());

		if (!Files.exists(encodedContentPath)) {
			return null;
		}

		return new BufferedInputStream(Files.newInputStream(encodedContentPath));
	}

	@Override
	public InputStream read(Hash hash) throws IOException {
		if (hash == null)
			throw new IllegalArgumentException("hash");

		Path contentPath = getContentPath(hash.getString());

		if (!Files.exists(contentPath)) {
			return null;
		}

		return new BufferedInputStream(Files.newInputStream(contentPath));
	}

	@Override
	public long getContentLength(Hash hash) throws IOException {
		if (hash == null)
			throw new IllegalArgumentException("hash");

		Path contentPath = getContentPath(hash.getString());

		if (!Files.exists(contentPath)) {
			return -1;
		}

		BasicFileAttributes attrs;
		attrs = Files.readAttributes(contentPath, BasicFileAttributes.class);
		return attrs.size();
	}

	@Override
	public long getContentLength(Hash hash, ContentEncoding contentEncoding) throws IOException {
		if (hash == null)
			throw new IllegalArgumentException("hash");

		if (contentEncoding == null) {
			return getContentLength(hash);
		}

		Path encodedContentPath = getContentPath(hash.getString() + "." + contentEncoding.getName());

		if (!Files.exists(encodedContentPath)) {
			return -1;
		}

		BasicFileAttributes attrs;
		attrs = Files.readAttributes(encodedContentPath, BasicFileAttributes.class);
		return attrs.size();
	}

	@Override
	public List<Hash> getHashes() throws IOException {
		List<Hash> hashes = new LinkedList<Hash>();

		try (DirectoryStream<Path> directories = Files.newDirectoryStream(rootPath, "[0-9A-F]*");) {
			for (Path directory : directories) {
				try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "[0-9A-F]*");) {
					for (Path file : files) {
						// Don't add in any encoded names
						if (file.getFileName().toString().contains(".")) {
							continue;
						}
						Hash hash = new Hash(directory.getFileName().toString() + file.getFileName().toString());
						hashes.add(hash);
					}
				}
			}
			return hashes;
		}
	}

	/**
	 * Class for wrapping IOExceptions so they become unchecked and thus
	 * throwable from inside a lambda in a stream.
	 *
	 */
	private class UncheckedIOException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public IOException ioException;

		public UncheckedIOException(IOException ioException) {
			this.ioException = ioException;
		}
	}

	@Override
	public boolean delete(Hash hash) throws IOException {
		String hashString = hash.getString();
		Path contentPath = getContentPath(hashString);

		if (!Files.exists(contentPath))
			return false;

		Path dirPath = getSubPath(hashString);
		// Java's streams are so broken when it comes to checked exceptions...
		try {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath,
					hashString.substring(hashPrefixLength) + "*")) {
				stream.forEach(file -> {
					try {
						Files.delete(file);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
			}
		} catch (UncheckedIOException e) {
			throw e.ioException;
		}
		// And delete the directory if it is empty now
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
			if (!stream.iterator().hasNext()) {
				Files.delete(dirPath);
			}
		}

		notifyListenersContentRemoved(hash);
		return true;
	}

	public Path getContentPath(String hashString) {
		Path subPath = getSubPath(hashString);
		Path contentPath = subPath.resolve(hashString.substring(hashPrefixLength));
		return contentPath;
	}

	public Path getSubPath(String hashString) {
		Path subPath = rootPath.resolve(hashString.substring(0, hashPrefixLength));
		return subPath;
	}

	@Override
	public void close() {
		// Nothing to do here.
		// Could delete all the content we own?
	}
}
