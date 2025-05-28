import java.io.*;
import java.net.*;
import java.util.*;

public class client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 5000;
    private static final int TOTAL_PACKETS = 10;

    private static final double ALPHA = 0.125;
    private static final double BETA = 0.25;

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket(SERVER_IP, PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected to server at port " + PORT);

        double estimatedRTT = 100.0;
        double devRTT = 0.0;
        double timeout = estimatedRTT;

        int acked = 0;
        Map<Integer, Long> sendTimes = new HashMap<>();
        Map<Integer, Integer> duplicateAcks = new HashMap<>();

        socket.setSoTimeout((int) timeout);

        while (acked < TOTAL_PACKETS) {
            int seq = acked + 1;
            long sendTime = System.currentTimeMillis();
            System.out.println("Sending Packet " + seq + " with Seq# " + seq);
            out.println(seq);
            sendTimes.put(seq, sendTime);

            try {
                socket.setSoTimeout((int) timeout);
                String response = in.readLine();
                int ack = Integer.parseInt(response);

                if (ack == seq) {
                    long sampleRTT = System.currentTimeMillis() - sendTimes.get(seq);
                    estimatedRTT = (1 - ALPHA) * estimatedRTT + ALPHA * sampleRTT;
                    devRTT = (1 - BETA) * devRTT + BETA * Math.abs(sampleRTT - estimatedRTT);
                    timeout = estimatedRTT + 4 * devRTT;

                    System.out.printf("ACK %d received. RTT = %dms. EstimatedRTT = %.1fms, DevRTT = %.1fms, Timeout = %.1fms\n",
                            ack, sampleRTT, estimatedRTT, devRTT, timeout);

                    acked++;
                    duplicateAcks.clear();
                } else {
                    System.out.println("Received Duplicate ACK for Seq " + ack);
                    duplicateAcks.put(ack, duplicateAcks.getOrDefault(ack, 0) + 1);
                    if (duplicateAcks.get(ack) == 3) {
                        System.out.println("Fast Retransmit Triggered for Packet " + seq);
                        out.println(seq);
                        sendTimes.put(seq, System.currentTimeMillis());
                    }
                }

            } catch (SocketTimeoutException e) {
                System.out.println("Timeout! Resending Packet " + seq);
                out.println(seq);
                sendTimes.put(seq, System.currentTimeMillis());
            }
        }

        System.out.println("ACK " + TOTAL_PACKETS + " received. All packets delivered successfully!");
        socket.close();
    }
}
