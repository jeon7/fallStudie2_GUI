package chat.server;

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
	private Semaphore readingSemaphore;
	private Semaphore writingSemaphore;
	
	private static ArrayList<Message> messageList = new ArrayList<>();
	private String username;
	
	protected static String register_command = "register#";
    protected static String send_command = "send#";
    protected static String get_command = "get#";
    protected static String exit_command = "exit#";
	
	public ConnectionHandler(Socket connect_socket, Semaphore readingSemaphore, Semaphore writingSemaphore) {
		this.connect_socket = connect_socket;
		this.readingSemaphore = readingSemaphore;
		this.writingSemaphore = writingSemaphore;
	}
	
	@Override
	public void run() {

        try {
			System.out.println("Client connected: " + connect_socket.getInetAddress());
			BufferedReader reader = new BufferedReader(new InputStreamReader(connect_socket.getInputStream()));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connect_socket.getOutputStream()));
			
			while(!connect_socket.isClosed()){
			    String client_message = reader.readLine();
			    String command = null;
				String content = null;
			    
			    if(client_message == null){
			        System.out.println("no response from client. closing connect_socket.");
			        break;
			    }
			    
			    if(client_message.startsWith(register_command)) {
			        registerUser_addToMessegeList(client_message);
			    } else if(client_message.startsWith(send_command)) {
					chatMessage_addToMessageList(client_message);
			    } else if(client_message.startsWith(exit_command)) {
			    	exitMessage_addToMessageList();
			        break;
			    } else if(client_message.startsWith(get_command)) {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("InterruptedException caused by Semaphore");
		}
	}

	private void registerUser_addToMessegeList(String client_message) throws IOException, InterruptedException {
		String content = client_message.substring(client_message.indexOf("#")+1);
		this.username = content;
		writingSemaphore.acquire(1);
		messageList.add(new Message(username, register_command, content));
		writingSemaphore.release(1);

	}
	
	private void chatMessage_addToMessageList(String client_message) throws IOException, InterruptedException {
		String content = client_message.substring(client_message.indexOf("#")+1);
		writingSemaphore.acquire(1);
		messageList.add(new Message(username, send_command, content));
		writingSemaphore.release(1);
	}
	
	private void exitMessage_addToMessageList() throws IOException, InterruptedException {
		writingSemaphore.acquire(1);
		messageList.add(new Message(username, exit_command, null));
		writingSemaphore.release(1);
		
		// for test
		System.out.println("disconnetion request. socket closing");
	}
	
	// from messageList(arrayList), which contains all history of register#, send#, exit#
    // to chatCourse(stringBuffer), which to send client. 
    // number of message comes from client_message.(get#number)
	private void writeChatCourse(String client_message, BufferedWriter writer) throws IOException, InterruptedException {
		int amount_lines;
		String chatSingleMessege;
		StringBuffer chatCourse = new StringBuffer();
		while (true) {
			try {
				amount_lines = Integer.parseInt(client_message.substring(client_message.indexOf("#")+1));
				
				readingSemaphore.acquire(1);
				if (amount_lines >= messageList.size()) {
					for (int i = 0; i < messageList.size(); i++) {
						chatSingleMessege = messageList.get(i).getMessage();
						chatCourse.append(chatSingleMessege);
						chatCourse.append(System.lineSeparator());
					}
				} else if (amount_lines < messageList.size()) {
					for (int i = messageList.size() - amount_lines; i < messageList.size(); i++) {
						chatSingleMessege = messageList.get(i).getMessage();
						chatCourse.append(chatSingleMessege);
						chatCourse.append(System.lineSeparator());
					}
				}
				readingSemaphore.release(1);
				
				System.out.println(chatCourse.toString()); // test
				writer.write(chatCourse.toString());
				writer.newLine();
				writer.flush();
				chatCourse.delete(0, chatCourse.length());
				break;
			} catch (NumberFormatException e) {
				writer.write("wrong number format.");
				writer.newLine();
				writer.flush();
				continue;
			}
		}	
	} 
}
