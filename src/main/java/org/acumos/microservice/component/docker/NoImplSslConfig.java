package org.acumos.microservice.component.docker;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;

import com.github.dockerjava.core.SSLConfig;

/**
 * A marker implementation of {@link SSLConfig}
 */
public class NoImplSslConfig implements SSLConfig {

    @Override
    public SSLContext getSSLContext() throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        return null;
    }

}
