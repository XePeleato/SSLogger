package HideawayLogger;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class App extends Application {

    public TextArea txtPacket;
    public Button btnSendToServer;
    public Button btnSendToClient;
    public CheckBox cbMute;

    private static HideawayLogger Logger;

    public static void main( String[] args )
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Injector.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("XeHideawayLogger");
        primaryStage.setScene(new Scene(root, 598, 238));
        primaryStage.show();

        new Thread(() -> {
            Logger = new HideawayLogger();
            Logger.run();
        }).start();

    }

    public void onBtnSendToServerClick(ActionEvent actionEvent) {
        HPacket send = new HPacket(txtPacket.getText());
        if (!send.isCorrupted())
            Logger.getDefaultSession().sendToServerAsync(send.toBytes());
    }

    public void onBtnSendToClientClick(ActionEvent actionEvent) {
        HPacket send = new HPacket(txtPacket.getText());
        if (!send.isCorrupted())
            Logger.getDefaultSession().sendToClientAsync(send.toBytes());
    }

    public void onCbMuteToggle(ActionEvent actionEvent) {
        Logger.getDefaultSession().getLogger().toggleMuteIncoming(cbMute.isSelected());
    }


}
