package io.github.gcdd1993.casdemo.app1.config;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.TrustStrategy;
import org.jasig.cas.client.ssl.HttpURLConnectionFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class InsecureHttpsURLConnectionFactory implements HttpURLConnectionFactory {
    private final SSLContext sslContext = trustAllSSLContext();

    public InsecureHttpsURLConnectionFactory() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    }

    @Override
    public HttpURLConnection buildHttpURLConnection(URLConnection conn) {
        if (conn instanceof HttpsURLConnection) {
            final HttpsURLConnection httpsConnection = (HttpsURLConnection) conn;
            httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
            httpsConnection.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }
        return (HttpURLConnection) conn;
    }

    public SSLContext trustAllSSLContext() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        return org.apache.http.ssl.SSLContexts.custom()
            .loadTrustMaterial(null, acceptingTrustStrategy)
            .build();
    }
}
