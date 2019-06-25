package ch.teko.GJSB.chat.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class ConnectionHandler extends Thread {

	private Socket connect_socket;
	private Semaphore semaphore;
	private String username;
	private static ArrayList<Message> messageList = new ArrayList<>(); // all chat history is saved as Message object.
	protected static String register_command = "register#";
	protected static String send_command = "send#";
	protected static String get_command = "get#";
	protected static String exit_command = "exit#";

	public ConnectionHandler(Socket connect_socket, Semaphore semaphore) {
		this.connect_socket = connect_socket;
		this.semaphore = semaphore;
	}

	@Override
	public void run() {

		try {
			System.out.println("Client connected: " + connect_socket.getInetAddress());
			BufferedReader reader = new BufferedReader(new InputStreamReader(connect_socket.getInputStream()));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connect_socket.getOutputStream()));

			while (!connect_socket.isClosed()) {
				String client_message = reader.readLine();
				
				// message interpretation
				if (client_message == null) {
					System.out.println("no response from client. closing connect_socket.");
					break;
				}

				if (client_message.startsWith(register_command)) {
					addUser(client_message);
				} else if (client_message.startsWith(send_command)) {
					addMessage(client_message);
				} else if (client_message.startsWith(exit_command)) {
					addExitMessage();
					break;
				} else if (client_message.startsWith(get_command)) {
					writeChatCourse(client_message, writer);
				} else {
					System.out.println("Unknown message: " + client_message);
					writer.write("Unknown message: " + client_message);
					writer.newLine();
					writer.flush();
				}
			}

			connect_socket.close();
			Server.current_connection.decrementAndGet();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("InterruptedException caused by Semaphore");
		}
	}
	
	// method to add 'register#[user]' to MessageList
	private void addUser(String client_message) throws IOException, InterruptedException {
		String content = client_message.substring(client_message.indexOf("#") + 1);
		this.username = content;
		semaphore.acquire(Server.ReadingLimit);
		messageList.add(new Message(username, register_command, content));
		semaphore.release(Server.ReadingLimit);
	}

	// method to add 'send#[message]' to MessageList
	private void addMessage(String client_message) throws IOException, InterruptedException {
		String content = client_message.substring(client_message.indexOf("#") + 1);
		semaphore.acquire(Server.ReadingLimit);
		messageList.add(new Message(username, send_command, content));
		semaphore.release(Server.ReadingLimit);
	}

	// method to add 'exit#[*]' to MessageList
	private void addExitMessage() throws IOException, InterruptedException {
		semaphore.acquire(Server.ReadingLimit);
		messageList.add(new Message(username, exit_command, null));
		semaphore.release(Server.ReadingLimit);
	}

	// builds a single line of text
	// from messageList(arrayList), which contains all history of register#, send#, exit#
	// to chatCourse(stringBuffer), which to send client.
	// number of message(amount_lines) comes from client_message.(get#number)
	private void writeChatCourse(String client_message, BufferedWriter writer)
			throws IOException, InterruptedException {
		int amount_lines;
		String chatSingleMessege;
		StringBuffer chatCourse = new StringBuffer();
		try {
			amount_lines = Integer.parseInt(client_message.substring(client_message.indexOf("#") + 1));

			semaphore.acquire(1);
			if (amount_lines >= messageList.size()) {
				chatCourse.append(send_command);
				for (int i = 0; i < messageList.size(); i++) {
					chatSingleMessege = messageList.get(i).getMessage();
					chatCourse.append(chatSingleMessege + "|");
				}
			} else if (amount_lines < messageList.size()) {
				chatCourse.append(send_command);
				for (int i = messageList.size() - amount_lines; i < messageList.size(); i++) {
					chatSingleMessege = messageList.get(i).getMessage();
					chatCourse.append(chatSingleMessege + "|");
				}
			}
			semaphore.release(1);

			writer.write(chatCourse.toString());
			writer.newLine();
			writer.flush();
			
		} catch (NumberFormatException e) {
			writer.write("error#wrong number format.");
			writer.newLine();
			writer.flush();
		}

	}
}
