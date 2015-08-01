package org.sturrock.cassette.cassettej;

import java.util.EventObject;

public class ContentAddressableStoreEvent extends EventObject {

	private Hash hash;
	/**
	 * UID
	 */
	private static final long serialVersionUID = 4212607284306663338L;

	/**
	 * Constructor
	 * 
	 * @param source
	 *            The store that generated the event
	 * @param hash
	 *            The hash associated with the event
	 */
	public ContentAddressableStoreEvent(ContentAddressableStore source,
			Hash hash) {
		super(source);
		this.hash = hash;
	}

	/**
	 * Each event is associated with a hash. This method returns that hash.
	 * 
	 * @return The hash associated with the event
	 */
	public Hash getHash() {
		return hash;
	}
}
