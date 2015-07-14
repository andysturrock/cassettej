package org.sturrock.cassette.cassettej;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

/**
 * A content-addressable store backed by the file system. Default implementation
 * of ContentAddressableStore.
 * 
 */
public final class ContentAddressableStoreFileImpl implements
		ContentAddressableStore {
	/**
	 * The root path for all content within this store
	 */
	private final Path rootPath;

	/**
	 * The size of the byte array buffer used for read/write operations.
	 */
	private final int bufferSize = 4096;

	/**
	 * The number of characters from the hash to use for the name of the top
	 * level subdirectories.
	 */
	private final int hashPrefixLength = 4;
	
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
		if(rootPath == null || rootPath.equals("")) {
			throw new IllegalArgumentException("No property " + rootPathPropertyName + " found");
		}
		this.rootPath = Paths.get(rootPath);

		if (!Files.isDirectory(this.rootPath))
			Files.createDirectories(this.rootPath);
	}

	@Override
	public Hash write(InputStream inputStream) throws IOException {
		if (inputStream == null)
			throw new IllegalArgumentException("inputStream");

		Path tmpFile = Files.createTempFile("CassetteJ", ".tmp");
		try {
			Files.copy(inputStream, tmpFile, StandardCopyOption.REPLACE_EXISTING);
		}
		catch(Exception e) {
			Files.delete(tmpFile);
			throw new IOException(e);
		}

		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}

		try(InputStream fileInputStream = new FileInputStream(tmpFile.toFile());) {
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

		// Test whether a file already exists for this hash
		if (!Files.exists(contentPath)) {
			// Ensure the sub-path exists
			if (!Files.isDirectory(subPath))
				Files.createDirectories(subPath);

			Files.move(tmpFile, contentPath);
		} else {
			Files.delete(tmpFile);
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
	public InputStream read(Hash hash) throws FileNotFoundException {
		if (hash == null)
			throw new IllegalArgumentException("hash");

		Path contentPath = getContentPath(hash.getString());

		if (!Files.exists(contentPath)) {
			return null;
		}

		return new FileInputStream(contentPath.toFile());
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
	public List<Hash> getHashes() throws IOException  {
		List<Hash> hashes = new LinkedList<Hash>();

		try(DirectoryStream<Path> directories = Files.newDirectoryStream(rootPath,
				"[0-9A-F]*");) {
				for (Path directory : directories) {
					try(DirectoryStream<Path> files = Files.newDirectoryStream(directory,
							"[0-9A-F]*");) {
					for (Path file : files) {
						Hash hash = new Hash(directory.getFileName().toString()
								+ file.getFileName().toString());
						hashes.add(hash);
					}
				}
			}
			return hashes;
		}
	}

	@Override
	public boolean delete(Hash hash) throws IOException {
		Path contentPath = getContentPath(hash.getString());

		if (!Files.exists(contentPath))
			return false;

		Files.delete(contentPath);
		return true;
	}

	private Path getContentPath(String hashString) {
		Path subPath = getSubPath(hashString);
		Path contentPath = Paths.get(subPath.toString(),
				hashString.substring(hashPrefixLength));
		return contentPath;
	}

	private Path getSubPath(String hashString) {
		Path subPath = Paths.get(rootPath.toString(),
				hashString.substring(0, hashPrefixLength));
		return subPath;
	}
	
	@Override
	public void close() {
		// Nothing to do here.
		// Could delete all the content we own?
	}
}
