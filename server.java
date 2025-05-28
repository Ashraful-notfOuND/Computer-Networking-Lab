import java.io.*;
import java.net.*;
import java.util.Random;

public class server {
    private static final int PORT = 5000;
    private static final double LOSS_PROBABILITY = 0.1;
    private static final int BUFFER_SIZE = 1024;
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server listening on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private int expectedSeq = 1;
        private Random rand = new Random();
        private int rcvWindow = BUFFER_SIZE;
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String input;
                while ((input = in.readLine()) != null) {
                    int seq = Integer.parseInt(input);

                    if (rand.nextDouble() < LOSS_PROBABILITY) {
                        System.out.println("— Packet " + seq + " lost —");
                        continue;
                    }

                    if (seq == expectedSeq) {
                        System.out.println("Received Packet " + seq + ". Sending ACK " + seq);
                        out.println(seq);
                        expectedSeq++;
                    } else {
                        System.out.println("Received Packet " + seq + ". Sending Duplicate ACK " + (expectedSeq - 1));
                        out.println(expectedSeq - 1);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected");
            }
        }
    }
}
