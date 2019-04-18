package HideawayLogger;

import javafx.collections.transformation.SortedList;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;

public class HideawayLogger implements Runnable {
    private DebugSession defaultSession;
    @Override
        public void run() {
        try {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("hideaway2.jks"), "G-EarthRocks".toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, "G-EarthRocks".toCharArray());
        SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(kmf.getKeyManagers(), null, null);

        SSLParameters params = sc.getSupportedSSLParameters();
        params.setCipherSuites(new String[] {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"});
        SSLServerSocketFactory ssf = sc.getServerSocketFactory();
        SSLServerSocket ss = (SSLServerSocket) ssf.createServerSocket(9999);
        ss.setEnabledCipherSuites(new String[] {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"});
        ss.setSSLParameters(params);

        while (true) {
            System.out.println("[+] Waiting...");
            SSLSocket client = (SSLSocket) ss.accept();

            SNIMatcher matcher = SNIHostName.createSNIMatcher("server225.hotelhideawaythegame.com");
            Collection<SNIMatcher> matchers = new ArrayList<>(1);
            matchers.add(matcher);

            params.setSNIMatchers(matchers);
            client.setSSLParameters(params);
            client.setEnabledCipherSuites(new String[] {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"});

            System.out.println("[+] Connected");
            client.startHandshake();
            printSocketInfo(client);
            defaultSession = new DebugSession(client);
            defaultSession.StartSession();

        }
        } catch (IOException | CertificateException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            e.printStackTrace();
        }
        System.out.println("[~] bye");
    }

    private static void printSocketInfo(SSLSocket s) {
        System.out.println("Socket class: " + s.getClass());
        System.out.println("Remote addr: " + s.getInetAddress().toString());
        System.out.println("Remote port: " + s.getPort());
        SSLSession ss  = s.getSession();
        System.out.println("Cipher suite: " + ss.getCipherSuite());
        System.out.println("Protocol: " + ss.getProtocol());
    }

    public DebugSession getDefaultSession() {
        return defaultSession;
    }
}
