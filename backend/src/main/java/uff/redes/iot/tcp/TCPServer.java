package uff.redes.iot.tcp;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import uff.redes.iot.dht.model.*;

import uff.redes.iot.dht.service.DHTService;
import uff.redes.iot.dht.service.DHTStatsService;
import uff.redes.iot.networkstats.NetworkStats;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;


@Component
@RequiredArgsConstructor
public class TCPServer {

    private final SimpMessagingTemplate messagingTemplate;
    private final DHTService service;
    private final DHTStatsService statsService;

    private volatile DHTResponse lastData = new DHTResponse(
            null, 0.0, 0.0, "Nenhum", ""
    );

    public DHTResponse getLastData() {
        return lastData;
    }

    @PostConstruct
    public void startServer() {
        new Thread(() -> runTCPServer()).start();
    }

    private void runTCPServer() {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("📡 Servidor TCP aguardando...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> processClient(clientSocket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processClient(Socket socket) {
        NetworkStats stats = statsService.getNetworkStats();
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {

                if (line.isBlank()) continue;
                System.out.println("📥 Recebido: " + line);

                String[] parts = line.split(",");
                if (parts.length != 4) continue;

                double temp = Double.parseDouble(parts[0]);
                double hum = Double.parseDouble(parts[1]);
                String origem = parts[2];
                long timestampESP = Long.parseLong(parts[3]);

                long timestampServidor = System.currentTimeMillis();
                long rtt = timestampServidor - timestampESP;

                int bytesRecebidos = line.getBytes().length;
                lastData = new DHTResponse(
                        null, temp, hum,
                        origem + " | RTT: " + rtt + " ms",
                        LocalDateTime.now().toString()
                );

                // delega responsabilidade ao serviço
                service.processIncomingData(temp, hum, origem);

                // WebSocket
                messagingTemplate.convertAndSend("/topic/dht", lastData);


                statsService.addRTT(rtt, bytesRecebidos);
                System.out.println("📊 Network Stats:");
                System.out.println("  - Throughput: " + stats.throughput() + " bytes/seg");
                System.out.println("  - Jitter: " + stats.jitter() + " ms");


                out.println("ACK," + timestampServidor);
            }

        } catch (Exception ignored) {}
    }
}
