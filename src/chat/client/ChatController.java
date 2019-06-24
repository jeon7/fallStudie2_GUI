package chat.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ChatController implements Initializable {

	// Attributes for the Scene Builder
	@FXML
	public TextField username;
	@FXML
	public Button connect;
	@FXML
	public TextArea messages;
	@FXML
	public TextField send_message;
	@FXML
	public Button send;

	// Other Attributes
	protected Socket connection;
	private static String send_command = "send#";
	private static String get_command = "get#";
	private static String registr_command = "register#";
	private static int amount_lines = 50;
	private static Thread t1;
	private static Object waiter = new Object();
	private boolean exit = false;

	// Initialization Methode
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Disable Button as long as the connection to the server was not established
		// and the username was sent
		send.setDisable(true);

		// Bind between the textfield username and the connect button, that the button
		// is only enabled if the textfield is not empty
		BooleanBinding binding = new BooleanBinding() {
			{
				super.bind(username.textProperty());
			}

			@Override
			protected boolean computeValue() {
				return (username.getText().isEmpty());
			}
		};
		connect.disableProperty().bind(binding);
	}

	// Method to create a connection to the socket and send the registration request
	public void sendUsername(ActionEvent event) {
		if (connection != null) {
			messages.appendText("Du bist bereits verbunden");
		} else {
			try {
				connection = new Socket(InetAddress.getByName("127.0.0.1"), 1300);
				BufferedWriter buf_writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
				buf_writer.write(registr_command + username.getText());
				buf_writer.newLine();
				buf_writer.flush();
				// Create new Thread for the background refresh of the messages and start it
				t1 = new Thread(new Runnable() {
					@Override
					public void run() {
						while (!exit) {
							getMessages();
							// synchronize it with the sendMessage methode in order to receive an notify for
							// the thread
							synchronized (waiter) {
								try {
									waiter.wait(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						exit = false;
						try {
							connection.close();
							connection = null;
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				t1.start();
				// Disable the username field and enable the sending of messages
				username.setDisable(true);
				send.setDisable(false);
			} catch (IOException e) {
				messages.setText("Verbindung konnte nicht hergestellt werden!\nVersuche es später nochmals.");
			}
		}
	}

	// Method to send the message
	public void sendMessage(ActionEvent event) {
		if (connection == null) {
			messages.appendText("Du bist noch nicht mit dem Server verbunden!");
		} else {
			try {
				BufferedWriter buf_writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
				buf_writer.write(send_command + send_message.getText());
				buf_writer.newLine();
				buf_writer.flush();
				// Notify the thread running in the background to re-load the messages.
				synchronized (waiter) {
					waiter.notify();
				}
				// Clear the textfield
				send_message.clear();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void getMessages() {
		try {
			BufferedWriter buf_writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
			buf_writer.write(get_command + amount_lines);
			buf_writer.newLine();
			buf_writer.flush();
			// Read the response from the server
			BufferedReader buf_reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			
			//
			// gukhwa edit************************
			String singleMessege;
        	StringBuffer chatCourse = new StringBuffer();
        	while ((singleMessege = buf_reader.readLine()) != null) {
        	    chatCourse.append(singleMessege);
        	    chatCourse.append(System.lineSeparator());
        	    checkReaderReady(buf_reader); // if reader is ready within 1 sec. get that message from server.
        	    // if reader is not ready within 1 sec.
        	    // without this break, cannot exit this while loop before socket.close()
        	    if(!buf_reader.ready()) 
        	        break;
        	}
        	messages.setText(chatCourse.toString());
        	chatCourse.delete(0, chatCourse.length());
        	
//			String rec_message;
//			rec_message = buf_reader.readLine();
//			processMessage(rec_message);
        	
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	// check if reader is ready to read.
    // Method to exit while-loop without closing socket.
    private void checkReaderReady(BufferedReader reader) throws IOException {
        long timeStamp = System.currentTimeMillis();
        // keep checking if reader is ready to read
        while((System.currentTimeMillis() - timeStamp) < 1000){
            if(reader.ready())
                break;
        }       
    }

			//
			//*************************************
    
    
	// Methode to process the received message from the server
	private void processMessage(String rec_message) {
		try {
			// Split the message into the type of the message (e.g. send#hallo -> type
			// "send" payload "hallo")
			String message_type = rec_message.substring(0, (rec_message.indexOf("#")));
			String payload = rec_message.substring((rec_message.indexOf("#")) + 1, rec_message.length());
			switch (message_type) {
			case "send":
				// payload can contain multiple lines separated by a pipe. Replace the pipes
				// with new line characters
				messages.setText((payload.replace("|", "\n")));
				messages.setScrollTop(Double.MAX_VALUE);
				break;
			case "error":
				// stop background thread and notify user
				exit = true;
				messages.appendText(payload);
				break;
			default:
				messages.appendText("Nicht unterstüzte Nachricht von Server empfangen\n" + rec_message);
				break;
			}
		} catch (StringIndexOutOfBoundsException e) {
			messages.appendText("Nicht unterstüzte Nachricht von Server empfangen\n" + rec_message);
			;
		}
	}

	// Method is used to stop the thread and close the connection. Method is called
	// by the main class
	public void close() {
		try {
			if (connection != null) {
				BufferedWriter buf_writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
				buf_writer.write("exit#bye");
				buf_writer.newLine();
				buf_writer.flush();
				buf_writer.close();
				exit = true;
				connection.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
