package org.sturrock.cassette.cassettej;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

import org.infinispan.Cache;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentAddressableStoreInfinispanImplTest extends ContentAddressableStoreTest {

	@BeforeClass
	public static void setUpClass() throws IOException {
		Properties properties = new Properties();
		properties.put(ContentAddressableStoreInfinispanImpl.configFilePropertyName, "infinispan.xml");
		properties.put(ContentAddressableStoreInfinispanImpl.cacheNamePropertyName, "CassetteJ");
		cas = new ContentAddressableStoreInfinispanImpl(properties);
	}

	@AfterClass
	public static void tearDownClass() {
		cas.close();
	}

	@After
	public void tearDown() {
		// Clear the cache after each test. The cache is private so use
		// reflection.
		Field field;
		try {
			field = cas.getClass().getDeclaredField("cache");
			field.setAccessible(true);
			@SuppressWarnings("rawtypes")
			Cache cache = (Cache) field.get(cas);
			cache.clear();
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			Assert.fail("Failed to clear cache between tests");
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor() throws IOException {
		Properties properties = new Properties();
		try (ContentAddressableStore cas = new ContentAddressableStoreInfinispanImpl(properties);) {
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor2() throws IOException {
		Properties properties = new Properties();
		properties.put(ContentAddressableStoreInfinispanImpl.configFilePropertyName, "does_not_exist");
		try (ContentAddressableStore cas = new ContentAddressableStoreInfinispanImpl(properties);) {
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor3() throws IOException {
		Properties properties = new Properties();
		properties.put(ContentAddressableStoreInfinispanImpl.configFilePropertyName, "infinispan.xml");
		try (ContentAddressableStore cas = new ContentAddressableStoreInfinispanImpl(properties);) {
		}
	}

}
