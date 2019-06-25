package ch.teko.GJSB.chat.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
	private static int port = 1200;
	private static int connection_pool = 20; // limit number of connected clients
	protected static int ReadingLimit = 10;
	protected static AtomicInteger current_connection = new AtomicInteger(0);

	public static void main(String[] args) {
		Semaphore semaphore = new Semaphore(ReadingLimit);
		ConnectionHandler[] connectionThread = new ConnectionHandler[connection_pool];
		ServerSocket server_socket;

		try {
			server_socket = new ServerSocket(port);
			System.out.println("server socket created");

			// while loop to constantly check the ServerSocket for new connections.
			while (true) {
				Socket connect_socket = server_socket.accept();
				// create new ConnectionHandler object, and hands over connection.
				if (current_connection.get() < connection_pool) {
					connectionThread[current_connection.get()] = new ConnectionHandler(connect_socket, semaphore);
					connectionThread[current_connection.get()].start();
					current_connection.getAndIncrement();
				} else {
					// report error to client in case of no available connection.
					BufferedWriter writer = new BufferedWriter(
							new OutputStreamWriter(connect_socket.getOutputStream()));
					writer.write("error#All connections are occupided!");
					writer.newLine();
					writer.flush();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}