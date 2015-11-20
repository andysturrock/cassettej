package org.sturrock.cassette.cassettej;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.DeflaterOutputStream;

import org.apache.commons.io.IOUtils;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

public class ContentAddressableStoreInfinispanImpl extends ContentAddressableStoreImpl {

	private DefaultCacheManager cacheManager;

	public final static String configFilePropertyName = ContentAddressableStoreInfinispanImpl.class.getName()
			+ ".configFile";

	public final static String cacheNamePropertyName = ContentAddressableStoreInfinispanImpl.class.getName()
			+ ".cacheName";

	private class CacheValue {
		public byte[] raw;
		public Map<ContentEncoding, byte[]> encoded = new HashMap<ContentEncoding, byte[]>();
	}

	private Cache<Hash, CacheValue> cache;

	public ContentAddressableStoreInfinispanImpl(Properties properties) throws IOException {
		if (properties == null)
			throw new IllegalArgumentException("properties");

		String configFileName = properties.getProperty(configFilePropertyName);
		if (configFileName == null || configFileName.equals("")) {
			throw new IllegalArgumentException("No property " + configFilePropertyName + " found");
		}

		Path configFile = Paths.get(configFileName);
		if (!Files.isRegularFile(configFile))
			throw new IllegalArgumentException("No config file " + configFileName + " found");
		cacheManager = new DefaultCacheManager(configFileName, true);

		String cacheName = properties.getProperty(cacheNamePropertyName);
		if (cacheName == null || cacheName.equals("")) {
			throw new IllegalArgumentException("No property " + cacheNamePropertyName + " found");
		}

		cache = cacheManager.getCache(cacheName);
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

		byte[] bytes = IOUtils.toByteArray(inputStream);

		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}

		messageDigest.update(bytes);
		Hash hash = new Hash(messageDigest.digest());

		// Write the raw content to the cacheValue
		CacheValue cacheValue = new CacheValue();
		cacheValue.raw = bytes;

		// Add encoded content to the cacheValue
		for (ContentEncoding encoding : encodings) {
			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					DeflaterOutputStream encodedOutputStream = encoding.encode(outputStream);
					ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);) {
				IOUtils.copy(byteArrayInputStream, encodedOutputStream);
				// Ensure everything is written to the stream before
				// converting to byte array.
				encodedOutputStream.finish();
				cacheValue.encoded.put(encoding, outputStream.toByteArray());
			}
		}
		// And add the value to the cache
		CacheValue previous = cache.put(hash, cacheValue);

		// Don't bother testing whether the previous bytes are
		// equal to the written bytes - the hash should guarantee
		// that anyway. Just assume that if there is nothing returned
		// that it is new content and if something is returned it
		// will be the same as we just added.
		if (previous == null) {
			notifyListenersContentAdded(hash);
		}

		return hash;
	}

	@Override
	public boolean contains(Hash hash) {
		return cache.containsKey(hash);
	}

	@Override
	public boolean contains(Hash hash, ContentEncoding contentEncoding) {
		if (hash == null) {
			throw new IllegalArgumentException("hash");
		}
		if (contentEncoding == null) {
			return contains(hash);
		}

		CacheValue cacheValue = cache.get(hash);
		if (cacheValue == null) {
			return false;
		}
		byte[] bytes = cacheValue.encoded.get(contentEncoding);
		return (bytes != null);
	}

	@Override
	public InputStream read(Hash hash) throws FileNotFoundException {
		CacheValue cacheValue = cache.get(hash);
		if (cacheValue == null) {
			return null;
		}
		return new ByteArrayInputStream(cacheValue.raw);
	}

	@Override
	public InputStream read(Hash hash, ContentEncoding contentEncoding) throws FileNotFoundException {
		if (hash == null)
			throw new IllegalArgumentException("hash");
		// null contentEncoding means use no encoding
		if (contentEncoding == null) {
			return read(hash);
		}

		CacheValue cacheValue = cache.get(hash);
		if (cacheValue == null) {
			return null;
		}
		byte[] encoded = cacheValue.encoded.get(contentEncoding);
		if (encoded == null) {
			return null;
		}
		return new ByteArrayInputStream(encoded);
	}

	@Override
	public long getContentLength(Hash hash) throws IOException {
		CacheValue cacheValue = cache.get(hash);
		if (cacheValue == null) {
			return -1;
		}
		return cacheValue.raw.length;
	}

	@Override
	public long getContentLength(Hash hash, ContentEncoding contentEncoding) throws IOException {
		if (contentEncoding == null) {
			return getContentLength(hash);
		}
		CacheValue cacheValue = cache.get(hash);
		if (cacheValue == null) {
			return -1;
		}

		byte[] encoded = cacheValue.encoded.get(contentEncoding);
		if (encoded == null) {
			return -1;
		}
		return encoded.length;
	}

	@Override
	public List<Hash> getHashes() throws IOException {
		List<Hash> hashes = new LinkedList<Hash>();
		hashes.addAll(cache.keySet());
		return hashes;
	}

	@Override
	public boolean delete(Hash hash) throws IOException {
		CacheValue contentRemoved = cache.remove(hash);
		if (contentRemoved == null) {
			return false;
		}
		notifyListenersContentRemoved(hash);
		return true;
	}

	@Override
	public void close() {
		if (cacheManager != null)
			cacheManager.stop();
		cacheManager = null;
	}
}
