import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
            System.out.println("Type your messages (type 'bye' to exit):");

            // Thread to read server responses
            Thread readerThread = new Thread(() -> {
                try {
                    String response;
                    while ((response = input.readLine()) != null) {
                        System.out.println(response);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading from server: " + e.getMessage());
                }
            });
            readerThread.start();

            // Main thread for user input
            String userInput;
            while (true) {
                System.out.print("You: ");
                userInput = scanner.nextLine();

                output.println(userInput);

                if (userInput.equalsIgnoreCase("bye")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}