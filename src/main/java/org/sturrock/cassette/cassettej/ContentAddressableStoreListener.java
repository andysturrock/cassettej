package org.sturrock.cassette.cassettej;

import java.util.EventListener;

/**
 * Listens to changes to a ContentAddressableStore
 */
public interface ContentAddressableStoreListener extends EventListener {

	/**
	 * Notifies this listener that content has been added to the store.
	 * 
	 * @param contentAddressableStoreEvent
	 *            Event object containing information about the content.
	 */
	public void contentAdded(
			ContentAddressableStoreEvent contentAddressableStoreEvent);

	/**
	 * Notifies this listener that content has been removed from the store.
	 * 
	 * @param contentAddressableStoreEvent
	 *            Event object containing information about the content.
	 */
	public void contentRemoved(
			ContentAddressableStoreEvent contentAddressableStoreEvent);

}
