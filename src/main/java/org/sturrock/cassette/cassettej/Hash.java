package org.sturrock.cassette.cassettej;

import java.util.Arrays;
import java.util.Formatter;

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
 * Class representing a hash. Contains methods for translating between byte
 * array and string representations. Class is immutable, so thread-safe.
 *
 */
public final class Hash {

	public static final int byteCount = 20;

	private String string;
	private byte[] bytes;

	public Hash(String string) {
		this.string = new String(string);
		bytes = getBytes(this.string);
	}

	public Hash(byte[] bytes) {
		this.bytes = Arrays.copyOf(bytes, bytes.length);
		this.string = getString(this.bytes);
	}

	public String getString() {
		return new String(string);
	}

	public byte[] getBytes() {
		return Arrays.copyOf(bytes, bytes.length);
	}

	public static String getString(byte[] hash) {
		Formatter formatter = new Formatter();
		for (final byte b : hash) {
			formatter.format("%02X", b);
		}
		String hashString = formatter.toString();
		formatter.close();
		return hashString;
	}

	public static byte[] getBytes(String string) {
		int len = string.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4) + Character
					.digit(string.charAt(i + 1), 16));
		}
		return data;
	}

	public static boolean equals(byte[] hash1, byte[] hash2) {
		if (hash1 == null)
			throw new IllegalArgumentException("hash1");
		if (hash2 == null)
			throw new IllegalArgumentException("hash2");
		if (hash1.length != byteCount)
			throw new IllegalArgumentException("hash1 has invalid length.");
		if (hash2.length != byteCount)
			throw new IllegalArgumentException("hash2 hHas invalid length.");

		for (int i = 0; i < byteCount; ++i) {
			if (hash1[i] != hash2[i])
				return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
		result = prime * result + ((string == null) ? 0 : string.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Hash other = (Hash) obj;
		if (!Arrays.equals(bytes, other.bytes))
			return false;
		if (string == null) {
			if (other.string != null)
				return false;
		} else if (!string.equals(other.string))
			return false;
		return true;
	}

}
