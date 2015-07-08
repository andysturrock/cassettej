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
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.manager.DefaultCacheManager;

public class ContentAddressableStoreInfinispanImpl implements
ContentAddressableStore {
	
	private DefaultCacheManager manager;
	
	public final static String configFilePropertyName = ContentAddressableStoreFileImpl.class.getName() + ".configFile";
	
	public final static String cacheNamePropertyName = ContentAddressableStoreFileImpl.class.getName() + ".cacheName";
	
	private Cache<Hash, byte[]> cache;

	public ContentAddressableStoreInfinispanImpl(Properties properties) throws IOException {
		if (properties == null)
			throw new IllegalArgumentException("properties");

		String configFileName = properties.getProperty(configFilePropertyName);
		if(configFileName == null || configFileName.equals("")) {
			throw new IllegalArgumentException("No property " + configFilePropertyName + " found");
		}
		
		Path configFile = Paths.get(configFileName);
		if (!Files.isRegularFile(configFile))
			throw new IllegalArgumentException("No config file " + configFileName + " found");
         manager = new DefaultCacheManager(configFileName, true);
         
        String cacheName = properties.getProperty(cacheNamePropertyName);
 		if(cacheName == null || cacheName.equals("")) {
 			throw new IllegalArgumentException("No property " + cacheNamePropertyName + " found");
 		}
 		
        cache = manager.getCache(cacheName);
	}

	@Override
	public byte[] write(InputStream stream) throws IOException {
		
		byte[] bytes = IOUtils.toByteArray(stream);
		
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
		
		messageDigest.update(bytes);
		Hash hash = new Hash(messageDigest.digest());
		
		cache.put(hash, bytes);
		
		return hash.getBytes();
	}

	@Override
	public boolean contains(byte[] hashBytes) {
		Hash hash = new Hash(hashBytes);
		return cache.containsKey(hash);
	}

	@Override
	public InputStream read(byte[] hashBytes) throws FileNotFoundException {
		Hash hash = new Hash(hashBytes);
		byte[] bytes = cache.get(hash);
		return new ByteArrayInputStream(bytes);
	}

	@Override
	public long getContentLength(byte[] hashBytes) throws IOException {
		Hash hash = new Hash(hashBytes);
		byte[] bytes = cache.get(hash);
		return bytes.length;
	}

	@Override
	public List<byte[]> getHashes() throws IOException {
		List<byte[]> bytesList = new LinkedList<byte[]>();
		try (CloseableIterator<Hash> hashes = cache.keySet().iterator()) {
			while(hashes.hasNext()) {
				Hash hash = hashes.next();
				bytesList.add(hash.getBytes());
			}
		}
		return bytesList;
	}

	@Override
	public boolean delete(byte[] hashBytes) throws IOException {
		Hash hash = new Hash(hashBytes);
		return (cache.remove(hash) != null);
	}

}
