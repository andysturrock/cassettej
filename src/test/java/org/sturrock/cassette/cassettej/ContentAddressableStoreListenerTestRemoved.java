package org.sturrock.cassette.cassettej;

import static org.junit.Assert.fail;

class ContentAddressableStoreListenerTestRemoved implements
		ContentAddressableStoreListener {

	private ContentAddressableStoreEvent contentAddressableStoreEvent;

	@Override
	public void contentAdded(
			ContentAddressableStoreEvent contentAddressableStoreEvent) {
		// Should not get called
		fail();
	}

	@Override
	public void contentRemoved(
			ContentAddressableStoreEvent contentAddressableStoreEvent) {
		this.contentAddressableStoreEvent = contentAddressableStoreEvent;
	}

	public ContentAddressableStoreEvent getEvent() {
		return contentAddressableStoreEvent;
	}

}