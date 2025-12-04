package uff.redes.iot.tcp;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import uff.redes.iot.dht.model.DHTResponse;
import uff.redes.iot.dht.service.DHTService;
import uff.redes.iot.dht.service.DHTStatsService;
import uff.redes.iot.networkstats.NetworkStats;

import jakarta.annotation.PostConstruct;
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

    private volatile DHTResponse lastData = new DHTResponse(null, 0.0, 0.0, "Nenhum", "");

    public DHTResponse getLastData() {
        return lastData;
    }

    @PostConstruct
    public void startServer() {
        new Thread(this::runTCPServer).start();
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
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                System.out.println("📥 Recebido: " + line);

                String[] parts = line.split(",");
                if (parts.length != 6) {  // Agora enviamos RTT e Jitter
                    System.out.println("⚠️ Formato inválido. Esperados 6 campos, recebidos: " + parts.length);
                    continue;
                }

                double temp = Double.parseDouble(parts[0]);
                double hum = Double.parseDouble(parts[1]);
                String origem = parts[2];
                long timestampESP = Long.parseLong(parts[3]);
                long rttESP = Long.parseLong(parts[4]);
                long jitterESP = Long.parseLong(parts[5]);

                long timestampServidor = System.currentTimeMillis();

                lastData = new DHTResponse(
                        null, temp, hum,
                        origem + " | RTT: " + rttESP + " ms | Jitter: " + jitterESP + " ms",
                        LocalDateTime.now().toString()
                );

                service.processIncomingData(temp, hum, origem);

                messagingTemplate.convertAndSend("/topic/dht", lastData);

                int bytesRecebidos = line.getBytes().length;
                statsService.addRTT(rttESP, jitterESP, bytesRecebidos);

                NetworkStats stats = statsService.getNetworkStats();
                System.out.println("📊 Network Stats:");
                System.out.println("  - Throughput: " + stats.throughput() + " bytes/seg");
                System.out.println("  - Jitter (média do servidor): " + stats.jitter() + " ms");
                System.out.println("  - RTT atual (ESP): " + rttESP + " ms");

                out.println("ACK," + timestampServidor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
