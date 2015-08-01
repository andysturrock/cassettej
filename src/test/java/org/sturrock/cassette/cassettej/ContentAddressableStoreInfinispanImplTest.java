package org.sturrock.cassette.cassettej;

import java.io.IOException;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentAddressableStoreInfinispanImplTest extends
		ContentAddressableStoreTest {

	@BeforeClass
	public static void setUpClass() throws IOException {
		Properties properties = new Properties();
		properties.put(
				ContentAddressableStoreInfinispanImpl.configFilePropertyName,
				"infinispan.xml");
		properties.put(
				ContentAddressableStoreInfinispanImpl.cacheNamePropertyName,
				"CassetteJ");
		cas = new ContentAddressableStoreInfinispanImpl(properties);
	}

	@AfterClass
	public static void tearDownClass() {
		cas.close();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor() throws IOException {
		Properties properties = new Properties();
		try (ContentAddressableStore cas = new ContentAddressableStoreInfinispanImpl(
				properties);) {
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor2() throws IOException {
		Properties properties = new Properties();
		properties.put(
				ContentAddressableStoreInfinispanImpl.configFilePropertyName,
				"does_not_exist");
		try (ContentAddressableStore cas = new ContentAddressableStoreInfinispanImpl(
				properties);) {
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor3() throws IOException {
		Properties properties = new Properties();
		properties.put(
				ContentAddressableStoreInfinispanImpl.configFilePropertyName,
				"infinispan.xml");
		try (ContentAddressableStore cas = new ContentAddressableStoreInfinispanImpl(
				properties);) {
		}
	}

}
