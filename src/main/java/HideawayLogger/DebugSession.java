package HideawayLogger;

import javax.net.ssl.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.Queue;

class DebugSession {
    private final Queue<byte[]> sendToClientAsyncQueue = new LinkedList<>();
    private final Queue<byte[]> sendToServerAsyncQueue = new LinkedList<>();

    private SSLSocket toHideaway;
    private SSLSocket client;

    private BufferedInputStream dInHideaway;
    private BufferedOutputStream dOsHideaway;

    private BufferedInputStream dInClient;
    private BufferedOutputStream dOsClient;

    private PacketLogger Logger = new PacketLogger();

    private static TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
    };
    DebugSession(SSLSocket client)
    {
        this.client = client;
    }

    public PacketLogger getLogger() {
        return Logger;
    }

    private void sendToServer(byte[] packet)  {
        if (dOsClient == null) return;
        try {
            dOsClient.write(packet);
            dOsClient.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendToClient(byte[] packet) {
        if (dOsHideaway == null) {
            return;
        }
        try {
            dOsHideaway.write(packet);
            dOsHideaway.flush();
        } catch (IOException ignored) {}
    }

    public void sendToClientAsync(byte[] message) {
        synchronized (sendToClientAsyncQueue) {
            sendToClientAsyncQueue.add(message);
        }

    }
    public void sendToServerAsync(byte[] message) {
        synchronized (sendToServerAsyncQueue) {
            sendToServerAsyncQueue.add(message);
        }
    }

    void StartSession()
    {
        try {
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory ssf = sc.getSocketFactory();

            toHideaway = (SSLSocket) ssf.createSocket("server225.hotelhideawaythegame.com", 9999);
            System.out.println("[+] Socket to hideaway created. Handshake...");
            toHideaway.startHandshake();
            System.out.println("[+] Handshake succeeded");
        } catch (Exception ignored) {}

        new Thread(() -> {
            while (true) {
                byte[] packet;
                synchronized (sendToClientAsyncQueue) {
                    while ((packet = sendToClientAsyncQueue.poll()) != null) {
                        Logger.addIncomingMessage(packet);
                        sendToClient(packet);
                    }
                }
                try { Thread.sleep(1); } catch (InterruptedException e) {
                    e.printStackTrace(); } }
        }).start();

        new Thread(() -> {
        while (true) {
            byte[] packet;
            synchronized (sendToServerAsyncQueue) {
                while ((packet = sendToServerAsyncQueue.poll()) != null) {
                    sendToServer(packet);
                    Logger.addOutgoingMessage(packet);
                }
            }

            try { Thread.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }).start();

        System.out.println("[+] Starting debug session...");

        new Thread(() -> {
            try {
                dInHideaway = new BufferedInputStream(toHideaway.getInputStream());
                dOsHideaway = new BufferedOutputStream(client.getOutputStream());

                byte[] pLen = new byte[4];
                byte[] buf = new byte[1024];
                while (!toHideaway.isClosed()) {

                    int amount = dInHideaway.read(buf);

                    byte[] ret = new byte[amount];

                    for (int i = 0; i < amount; i++)
                        ret[i] = buf[i];
                    sendToClientAsync(ret);
                }
                } catch (Exception e) {
                        e.printStackTrace();
                }
            }).start();


            new Thread(() -> {
                try {
                    dInClient = new BufferedInputStream(client.getInputStream());
                    dOsClient = new BufferedOutputStream(toHideaway.getOutputStream());

                    byte[] packetLen = new byte[4];
                    while (!client.isClosed()) {
                        int amountRead = 0;

                        if (dInClient.read(packetLen, 0, 4) < 4)
                            System.out.println("[-] The client probably disconnected");

                        int packetLength = ByteBuffer.wrap(packetLen).getInt();

                        byte[] buf = new byte[4 + packetLength];

                        while (amountRead < packetLength) {
                            amountRead += dInClient.read(buf, 4 + amountRead, Math.min(dInClient.available(), packetLength - amountRead));
                        }

                        byte[] lengthBytes = ByteBuffer.allocate(4).putInt(packetLength).array();

                        System.arraycopy(lengthBytes, 0, buf, 0, lengthBytes.length);

                        sendToServerAsync(buf);
                    } }
            catch (Exception ignored) {}
            }).start();

    }
}
