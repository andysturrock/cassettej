package org.sturrock.cassette.cassettej;

import java.io.IOException;
import java.util.Properties;

public class ContentAddressableStoreFactory {

	public enum ImplementationType {
		FILE, INFINISPAN
	}

	public static ContentAddressableStore createContentAddressableStore(
			ImplementationType implementationType, Properties properties) {
		switch (implementationType) {
		case FILE:
			try {
				return new ContentAddressableStoreFileImpl(properties);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		case INFINISPAN:
			return new ContentAddressableStoreInfinispanImpl(properties);

		default:
			throw new IllegalArgumentException(
					"Unknown ContentAddressableStore implementation type: "
							+ implementationType.name());
		}
	}
}
