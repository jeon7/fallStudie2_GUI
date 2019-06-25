package chat.server;

public class Message {
	private String username = null;
	private String command = null;
	private String content = null;
	
	public Message(String user, String command, String content) {
		this.username = user;
		this.command = command;
		this.content = content;
	}
	
	public String getMessage() {
		String message;
		if (command == ConnectionHandler.register_command) {
			message = "'" + username + "' joined chatting.";
			return message;
		} else if (command == ConnectionHandler.send_command) {
			message = username + ": " + content;
			return message;
		} else if (command == ConnectionHandler.exit_command) {
			message = "'" + username + "' exited chatting.";
			return message;
		} else {
			System.out.println("error");
			message = "error";
			return message;
		}
	}
}
