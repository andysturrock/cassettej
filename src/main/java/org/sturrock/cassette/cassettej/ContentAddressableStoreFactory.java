package org.sturrock.cassette.cassettej;

import java.io.IOException;
import java.util.Properties;

public class ContentAddressableStoreFactory {

	public enum ImplementationType {
		FILE, INFINISPAN
	}

	public static ContentAddressableStore createContentAddressableStore(
			ImplementationType implementationType, Properties properties) throws IOException {
		switch (implementationType) {
		case FILE:
			return new ContentAddressableStoreFileImpl(properties);

		case INFINISPAN:
			return new ContentAddressableStoreInfinispanImpl(properties);

		default:
			throw new IllegalArgumentException(
					"Unknown ContentAddressableStore implementation type: "
							+ implementationType.name());
		}
	}
}
