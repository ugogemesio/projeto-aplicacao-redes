package uff.redes.iot.tcp;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import uff.redes.iot.dht.model.DHTResponse;
import uff.redes.iot.dht.service.DHTService;
import uff.redes.iot.dht.service.DHTStatsService;
import uff.redes.iot.networkstats.model.NetworkStats;
import uff.redes.iot.networkstats.service.NetworkStatsService;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class TCPServer {

    private final SimpMessagingTemplate messagingTemplate;
    private final DHTService service;
    private final DHTStatsService statsService;
    private final NetworkStatsService networkStatsService;

    private final AtomicReference<DHTResponse> lastData = new AtomicReference<>(
            new DHTResponse(null, 0.0, 0.0, "Nenhum", "")
    );

    private final ExecutorService acceptPool = Executors.newCachedThreadPool();
//    Pool de threads para processar múltiplos clientes TCP simultaneamente.

    @PostConstruct
    public void startServer() {
        new Thread(this::runTCPServer, "tcp-server-main").start();
    }

//Cria um ServerSocket na porta 5000.
//serverSocket.accept() → espera por conexões de clientes.
//Cada cliente é processado em uma thread separada do pool (acceptPool.submit(...)).
//Isso permite vários ESPs ou dispositivos conectados ao mesmo tempo.
    private void runTCPServer() {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("📡 Servidor TCP aguardando...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                acceptPool.submit(() -> processClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor TCP: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processClient(Socket socket) {
//        Recupera o endereço remoto do cliente para logs.
        String remote = socket.getRemoteSocketAddress() != null ? socket.getRemoteSocketAddress().toString() : "unknown";
        try (
//                Cria streams para leitura e escrita de dados do socket:
//                BufferedReader → lê linhas do cliente.
//                PrintWriter → envia respostas.
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
//            Lê cada linha enviada pelo cliente.
//            Ignora linhas vazias.
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;

//                Se receber "PING", responde "ACK".
//                Serve como teste de conexão ou keep-alive.
                if ("PING".equals(line)) {
                    out.println("ACK");
                    continue;
                }

                System.out.println("📥 Recebido de " + remote + ": " + line);

//                Espera 6 campos separados por vírgula:
//                Temperatura
//                Umidade
//                Origem
//                Timestamp ESP
//                RTT ESP
//                Jitter ESP
                String[] parts = line.split(",");
                if (parts.length != 6) {
                    System.out.println("⚠️ Formato inválido. Esperados 6 campos, recebidos: " + parts.length);
                    continue;
                }

                try {
                    double temp = Double.parseDouble(parts[0]);
                    double hum = Double.parseDouble(parts[1]);
                    String origem = parts[2];
                    long timestampESP = Long.parseLong(parts[3]);
                    long rttESP = Long.parseLong(parts[4]);
                    long jitterESP = Long.parseLong(parts[5]);

                    long timestampServidor = System.currentTimeMillis();


//                    Atualização do último dado e envio para front-end
                    // Atualiza último dado (thread-safe)
                    DHTResponse newLast = new DHTResponse(
                            null, temp, hum,
                            origem ,
                            LocalDateTime.now().toString()
                    );
                    lastData.set(newLast);

                    // Processa dados de negócio (sensor)
                    service.processIncomingData(temp, hum, origem);

                    // Prefixo setado em WebConfig /topic envio
                    messagingTemplate.convertAndSend("/topic/dht", newLast);

                    // registra no statsService (existente)
                    int bytesRecebidos = line.getBytes().length;


                    // registra amostra no NetworkStatsService (para cálculo e persistência)
                    networkStatsService.registrarAmostra(rttESP, jitterESP, bytesRecebidos, timestampServidor);

                    // obtém métricas atuais (calculadas a partir do histórico)
                    NetworkStats stats = networkStatsService.getNetworkStats();

                    System.out.println("📊 Network Stats:");
                    System.out.println("  - Throughput: " + stats.throughput() + " bytes/seg");
                    System.out.println("  - Jitter (média do servidor): " + stats.jitter() + " µs");
                    System.out.println("  - RTT atual (ESP): " + stats.rtt() + " µs");

                    // responde com ACK + timestamp do servidor
                    out.println("ACK," + timestampServidor);

                } catch (NumberFormatException nfe) {
                    System.out.println("⚠️ Erro no parsing dos campos numéricos: " + nfe.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Conexão com cliente " + remote + " encerrada: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignore) { }
        }
    }

    // exposição segura do ultimo DHTResponse
    public DHTResponse getLastData() {
        return lastData.get();
    }
}
