package ch.teko.GJSB.chat.client;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatClient extends Application {
	
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("TEKO - Chat");
		try {FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("/main/resources/Layout.fxml"));
			Parent root = loader.load();
			ChatController controller = loader.getController();
			Scene scene = new Scene(root);
			primaryStage.setScene(scene);
			primaryStage.show();
			// Assign a methode to the close button which will notify the controller to close the connection
			primaryStage.setOnCloseRequest(e -> {
				controller.close();
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
