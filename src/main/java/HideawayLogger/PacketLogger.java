package HideawayLogger;



import javafx.scene.control.CheckBox;

import java.io.*;

public class PacketLogger {
    private boolean muteIncoming = false;
    public PacketLogger() {
    }

    public void toggleMuteIncoming(boolean status) {
        muteIncoming = status;
     }

    public void addIncomingMessage(byte[] msg) {
        new Thread(() -> {
            HPacket p = new HPacket(msg);
            if (!muteIncoming)
                System.out.println("[Incoming] (" + IncomingMessageCodes.getString(p.headerId()) + ") " + p.toString());

            try(FileWriter fw = new FileWriter("PacketLogger.txt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
                out.println("Incoming(" + IncomingMessageCodes.getString(p.headerId()) + ") ->" + p.toString());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void addOutgoingMessage(byte[] msg) {
        HPacket p = new HPacket(msg);
        if (p.headerId() != 1001) // there are just too many of these
            System.out.println("[Outgoing] (" + OutgoingMessageCodes.getString(p.headerId()) + ") " + p.toString());

        new Thread(() -> {
            try(FileWriter fw = new FileWriter("PacketLogger.txt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
                out.println("Outgoing(" + OutgoingMessageCodes.getString(p.headerId()) + ") ->" + p.toString());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
