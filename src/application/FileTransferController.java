package application;

import java.io.File;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

public class FileTransferController {

    @FXML
    private ImageView conView_indic;

    @FXML
    private ImageView downloadView_indic;

    @FXML
    private ImageView settingsView_indic;

    @FXML
    private AnchorPane downloadView;

    @FXML
    private ImageView button_download;

    @FXML
    private ImageView button_explorer;

    @FXML
    private ImageView button_refresh;

    @FXML
    private AnchorPane settingsView;

    @FXML
    private TextField textfield_dpath;

    @FXML
    private ImageView button_explorer2;

    @FXML
    private AnchorPane topbar;

    @FXML
    private ImageView openConView;

    @FXML
    private ImageView openDownloadView;

    @FXML
    private ImageView openSettingsView;

    @FXML
    private ImageView shutdown;

    @FXML
    private AnchorPane connectionView;

    @FXML
    private ImageView connectToServer;

    @FXML
    private ImageView conEstablished;

    @FXML
    private TextField textfield_port;

    @FXML
    private TextField textfield_ip;
    
    @FXML
    public void topBarIconClicked(ActionEvent e) {
    	ImageView source = (ImageView) e.getSource();
    	if(source.getId().equals("openConView")) {
    		this.connectionView.setVisible(true);
    	}
    }
    @FXML
	public void chooseDownloadDirectory(MouseEvent e) {
    	// get Stage
        Node source = (Node) e.getSource();
        Window stage = source.getScene().getWindow();
        
    	String input = "Bitte Pfad angeben";
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Download-Ordner angeben");
		File selectedDirectory = chooser.showDialog(stage);
		if(selectedDirectory != null) {
			textfield_dpath.setText(selectedDirectory.getAbsolutePath());
		}
	}
}
    	

