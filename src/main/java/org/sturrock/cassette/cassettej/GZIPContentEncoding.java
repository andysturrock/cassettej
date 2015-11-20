package org.sturrock.cassette.cassettej;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZIPContentEncoding implements ContentEncoding {

	public static final String name = "gzip";

	@Override
	public String getName() {
		return name;
	}

	@Override
	public DeflaterOutputStream encode(OutputStream stream) throws IOException {
		return new GZIPOutputStream(stream);
	}

	@Override
	public InputStream decode(InputStream stream) throws IOException {
		return new GZIPInputStream(stream);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GZIPContentEncoding other = (GZIPContentEncoding) obj;
		return this.getName().equals(other.getName());
	}
}
