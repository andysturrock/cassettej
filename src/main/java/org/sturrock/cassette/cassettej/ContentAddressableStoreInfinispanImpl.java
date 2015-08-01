package org.sturrock.cassette.cassettej;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

public class ContentAddressableStoreInfinispanImpl extends
		ContentAddressableStoreImpl {

	private DefaultCacheManager cacheManager;

	public final static String configFilePropertyName = ContentAddressableStoreInfinispanImpl.class
			.getName() + ".configFile";

	public final static String cacheNamePropertyName = ContentAddressableStoreInfinispanImpl.class
			.getName() + ".cacheName";

	private Cache<Hash, byte[]> cache;

	public ContentAddressableStoreInfinispanImpl(Properties properties)
			throws IOException {
		if (properties == null)
			throw new IllegalArgumentException("properties");

		String configFileName = properties.getProperty(configFilePropertyName);
		if (configFileName == null || configFileName.equals("")) {
			throw new IllegalArgumentException("No property "
					+ configFilePropertyName + " found");
		}

		Path configFile = Paths.get(configFileName);
		if (!Files.isRegularFile(configFile))
			throw new IllegalArgumentException("No config file "
					+ configFileName + " found");
		cacheManager = new DefaultCacheManager(configFileName, true);

		String cacheName = properties.getProperty(cacheNamePropertyName);
		if (cacheName == null || cacheName.equals("")) {
			throw new IllegalArgumentException("No property "
					+ cacheNamePropertyName + " found");
		}

		cache = cacheManager.getCache(cacheName);
	}

	@Override
	public Hash write(InputStream stream) throws IOException {

		byte[] bytes = IOUtils.toByteArray(stream);

		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}

		messageDigest.update(bytes);
		Hash hash = new Hash(messageDigest.digest());

		byte[] previous = cache.put(hash, bytes);
		// Don't bother testing whether the previous bytes are
		// equal to the written bytes - the hash should guarantee
		// that anyway.
		if (previous != null) {
			notifyListenersContentAdded(hash);
		}

		return hash;
	}

	@Override
	public boolean contains(Hash hash) {
		return cache.containsKey(hash);
	}

	@Override
	public InputStream read(Hash hash) throws FileNotFoundException {
		byte[] bytes = cache.get(hash);
		return new ByteArrayInputStream(bytes);
	}

	@Override
	public long getContentLength(Hash hash) throws IOException {
		byte[] bytes = cache.get(hash);
		return bytes.length;
	}

	@Override
	public List<Hash> getHashes() throws IOException {

		List<Hash> hashes = new LinkedList<Hash>();
		hashes.addAll(cache.keySet());
		return hashes;
	}

	@Override
	public boolean delete(Hash hash) throws IOException {
		byte[] contentRemoved = cache.remove(hash);
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
