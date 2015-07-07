package org.sturrock.cassette.cassettej;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class ContentAddressableStoreInfinispanImpl implements
ContentAddressableStore {

	public ContentAddressableStoreInfinispanImpl(Properties properties) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public byte[] write(InputStream stream) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(byte[] hash) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public InputStream read(byte[] hash) throws FileNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getContentLength(byte[] hash) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<byte[]> getHashes() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean delete(byte[] hash) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

}
