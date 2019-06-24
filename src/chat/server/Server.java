package chat.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static int port = 1300;
    private static int connection_pool = 3;
    protected static AtomicInteger current_connection = new AtomicInteger(0);
    private static int ReadingLimit = 2;
    private static int WritingLimit = 1;
	
    public static void main(String[] args) {
    	Semaphore readingSemaphore = new Semaphore(ReadingLimit);
		Semaphore writingSemaphore = new Semaphore(WritingLimit);
		ConnectionHandler[] connectionThread = new ConnectionHandler[connection_pool];
		ServerSocket server_socket;
		
        try {
            server_socket = new ServerSocket(port);
            System.out.println("server socket created");
 
            // while loop to constantly check the ServerSocket for new connections.
            while (true) {
				Socket connect_socket = server_socket.accept();
				if (current_connection.get() < connection_pool) {
					connectionThread[current_connection.get()] = new ConnectionHandler(connect_socket, readingSemaphore, writingSemaphore);
					connectionThread[current_connection.get()].start();
					current_connection.getAndIncrement();
				} else {
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connect_socket.getOutputStream()));
					writer.write("error#All connections are occupided!");
					System.out.println("error#All connections are occupided!");
					writer.flush();
					connect_socket.close();
				}
				Thread.sleep(1000);
			}
//            server_socket.close();
        } catch (IOException e) {
			// TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}