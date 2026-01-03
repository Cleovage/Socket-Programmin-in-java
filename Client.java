
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT); BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)); PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true); Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
            System.out.print("Enter your username: ");
            String username = scanner.nextLine().trim();
            if (username.isEmpty()) {
                username = "ConsoleUser";
            }

            // Send username first to match Server protocol
            output.println("USERNAME|" + username);

            System.out.println("Type your messages (type '/quit' or 'bye' to exit):");

            // Thread to read server responses
            Thread readerThread = new Thread(() -> {
                try {
                    String response;
                    while ((response = input.readLine()) != null) {
                        // Reply to heartbeat pings to avoid timeouts
                        if (response.startsWith("PING")) {
                            output.println("PONG");
                            continue;
                        }
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
                System.out.print(username + ": ");
                userInput = scanner.nextLine();

                if (userInput.equalsIgnoreCase("bye")) {
                    output.println("/quit");
                    break;
                }

                output.println(userInput);

                if (userInput.equalsIgnoreCase("/quit")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
