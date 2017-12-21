package at.usmile.cormorant.framework.messaging;

import org.whispersystems.signalservice.api.push.TrustStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class CormorantTrustStore implements TrustStore {

	@Override
	public InputStream getKeyStoreInputStream() {
		try {
			return new FileInputStream(new File("/opt/java/jdk1.8.0_111/jre/lib/security/cacerts"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getKeyStorePassword() {
		return "changeit";
	}

}
