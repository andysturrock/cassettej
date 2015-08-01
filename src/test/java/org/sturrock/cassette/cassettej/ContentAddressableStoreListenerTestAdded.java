package org.sturrock.cassette.cassettej;

import static org.junit.Assert.fail;

class ContentAddressableStoreListenerTestAdded implements
		ContentAddressableStoreListener {

	private ContentAddressableStoreEvent contentAddressableStoreEvent;

	@Override
	public void contentAdded(
			ContentAddressableStoreEvent contentAddressableStoreEvent) {
		this.contentAddressableStoreEvent = contentAddressableStoreEvent;
	}

	@Override
	public void contentRemoved(
			ContentAddressableStoreEvent contentAddressableStoreEvent) {
		// Should not get called
		fail();
	}

	public ContentAddressableStoreEvent getEvent() {
		return contentAddressableStoreEvent;
	}

}