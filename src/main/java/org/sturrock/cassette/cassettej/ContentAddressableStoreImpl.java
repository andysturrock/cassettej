package org.sturrock.cassette.cassettej;

import java.util.LinkedList;
import java.util.List;

public abstract class ContentAddressableStoreImpl implements
		ContentAddressableStore {

	private List<ContentAddressableStoreListener> listeners = new LinkedList<ContentAddressableStoreListener>();

	@Override
	public void addListener(
			ContentAddressableStoreListener contentAddressableStoreListener) {
		synchronized (listeners) {
			listeners.add(contentAddressableStoreListener);
		}
	}

	@Override
	public void removeListener(
			ContentAddressableStoreListener contentAddressableStoreListener) {
		synchronized (listeners) {
			listeners.remove(contentAddressableStoreListener);
		}
	}

	protected void notifyListenersContentAdded(Hash hash) {
		ContentAddressableStoreEvent contentAddressableStoreEvent = new ContentAddressableStoreEvent(
				this, hash);
		synchronized (listeners) {
			for (ContentAddressableStoreListener contentAddressableStoreListener : listeners) {
				contentAddressableStoreListener
						.contentAdded(contentAddressableStoreEvent);
			}
		}
	}

	protected void notifyListenersContentRemoved(Hash hash) {
		ContentAddressableStoreEvent contentAddressableStoreEvent = new ContentAddressableStoreEvent(
				this, hash);
		synchronized (listeners) {
			for (ContentAddressableStoreListener contentAddressableStoreListener : listeners) {
				contentAddressableStoreListener
						.contentRemoved(contentAddressableStoreEvent);
			}
		}
	}
}
