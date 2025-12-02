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
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                System.out.println("📥 Recebido: " + line);

                String[] parts = line.split(",");
                if (parts.length != 5) {  // Agora são 5 campos!
                    System.out.println("⚠️ Formato inválido. Esperados 5 campos, recebidos: " + parts.length);
                    continue;
                }

                double temp = Double.parseDouble(parts[0]);
                double hum = Double.parseDouble(parts[1]);
                String origem = parts[2];
                long timestampESP = Long.parseLong(parts[3]);
                long rttESP = Long.parseLong(parts[4]);  // RTT calculado pelo ESP

                long timestampServidor = System.currentTimeMillis();

                // ✅ Agora use o RTT que veio do ESP (já calculado corretamente)
                // Não precisa fazer timestampServidor - timestampESP

                int bytesRecebidos = line.getBytes().length;
                lastData = new DHTResponse(
                        null, temp, hum,
                        origem + " | RTT: " + rttESP + " ms",
                        LocalDateTime.now().toString()
                );

                // Delegar ao serviço
                service.processIncomingData(temp, hum, origem);

                // WebSocket
                messagingTemplate.convertAndSend("/topic/dht", lastData);

                // Usar o RTT que veio do ESP para estatísticas
                statsService.addRTT(rttESP, bytesRecebidos);

                NetworkStats stats = statsService.getNetworkStats();
                System.out.println("📊 Network Stats:");
                System.out.println("  - Throughput: " + stats.throughput() + " bytes/seg");
                System.out.println("  - Jitter: " + stats.jitter() + " ms");
                System.out.println("  - RTT atual (ESP): " + rttESP + " ms");

                // Enviar ACK de volta
                out.println("ACK," + timestampServidor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
