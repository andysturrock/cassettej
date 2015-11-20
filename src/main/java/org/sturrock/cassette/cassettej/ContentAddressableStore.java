package org.sturrock.cassette.cassettej;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/*
 * Copyright 2015 Andy Sturrock
 * Derived from https://github.com/drewnoakes/cassette
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Defines a content-addressable store
 */
public interface ContentAddressableStore extends AutoCloseable {
	/**
	 * Write content to the store, returning its hash. If the store already
	 * contains this content, the write is discarded but the hash is returned as
	 * normal.
	 * 
	 * @param stream
	 *            Content to be written
	 * @return hash of content
	 * @throws IOException
	 */
	Hash write(InputStream stream) throws IOException;

	/**
	 * Write content to the store in raw format and also formats specified in
	 * the ContentEncodings.  If encodings is null or an empty list then no
	 * encodings are used.
	 * 
	 * @param stream
	 *            Content to be written
	 * @return hash of (raw) content
	 * @throws IOException
	 */
	Hash write(InputStream stream, List<ContentEncoding> encodings) throws IOException;

	/**
	 * Check whether content exists in the store with the specified hash
	 * 
	 * @param hash
	 *            Hash of content to check
	 * @return <code>true</code> if content exists in the store with specified
	 *         hash
	 */
	boolean contains(Hash hash);
	
	/**
	 * Check whether content exists in the store with the specified hash with the specified encoding
	 * 
	 * @param hash
	 *            Hash of content to check
	 * @param contentEncoding
	 *            Type of encoding to use.
	 * @return <code>true</code> if content exists in the store with specified
	 *         hash and specified encoding.
	 */
	boolean contains(Hash hash, ContentEncoding contentEncoding);

	/**
	 * Read content from the store.
	 * 
	 * @param hash
	 *            The hash of the content to read.
	 * @return <code>InputStream</code> of content if content exists; otherwise
	 *         <code>null</code>.
	 * @throws IOException 
	 */
	InputStream read(Hash hash) throws IOException;

	/**
	 * Read content from the store, returning using the given encoding.
	 * 
	 * @param hash
	 *            The hash of the (raw) content to read.
	 * @param contentEncoding
	 *            Type of encoding to use to return the stream. If null then no
	 *            encoding is used.
	 * @return <code>InputStream</code> of content if content exists; otherwise
	 *         <code>null</code>.
	 * @throws IOException
	 */
	InputStream read(Hash hash, ContentEncoding contentEncoding) throws IOException;

	/**
	 * Get the length of the content with the specified hash
	 * 
	 * @param hash
	 *            The hash of the content
	 * @return The length of the content, or -1 if no content with the specified
	 *         hash exists.
	 * @throws IOException
	 */
	long getContentLength(Hash hash) throws IOException;

	/**
	 * Get the length of the encoded content with the specified hash.
	 * 
	 * @param hash
	 *            The hash of the content
	 * @param contentEncoding
	 *            The encoding type to use. If null then no encoding is used and
	 *            the raw content is assumed.
	 * @return The length of the content, or -1 if no content with the specified
	 *         hash exists.
	 * @throws IOException
	 */
	long getContentLength(Hash hash, ContentEncoding contentEncoding) throws IOException;

	/**
	 * Get a list of all hashes in the store. The list is generated lazily by
	 * querying the filesystem, and thus will not behave deterministically if
	 * more content is added while this function is called.
	 * 
	 * @return List of hashes in the store.
	 * @throws IOException
	 */
	List<Hash> getHashes() throws IOException;

	/**
	 * Delete content from the store.
	 * 
	 * @param hash
	 *            Hash of content to delete
	 * @return <code>true</code> if the content existed and was deleted;
	 *         otherwise false.
	 * @throws IOException
	 */
	boolean delete(Hash hash) throws IOException;

	/**
	 * Add a listener to this store.
	 * 
	 * @param contentAddressableStoreListener
	 */
	void addListener(ContentAddressableStoreListener contentAddressableStoreListener);

	/**
	 * Remove a listener to this store.
	 * 
	 * @param contentAddressableStoreListener
	 */
	void removeListener(ContentAddressableStoreListener contentAddressableStoreListener);

	@Override
	void close();
}
