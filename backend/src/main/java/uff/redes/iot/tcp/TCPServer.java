package uff.redes.iot.tcp;

import org.springframework.stereotype.Component;
import uff.redes.iot.dht.DHTResponse;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class TCPServer {

    private volatile DHTResponse lastData = new DHTResponse(
            null,    // id
            0.0,     // temperatura
            0.0,     // umidade
            "Nenhum",// origem
            ""       // dataHora
    );

    public DHTResponse getLastData() {
        return lastData;
    }

    @PostConstruct
    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(5000)) {
                System.out.println("Servidor TCP iniciado na porta 5000");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Cliente conectado: " + clientSocket.getInetAddress());

                    // Cada cliente em uma thread
                    new Thread(() -> handleClient(clientSocket)).start();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    // Cria um novo record a cada atualização
                    lastData = new DHTResponse(
                            null, // id
                            Double.parseDouble(parts[0]),
                            Double.parseDouble(parts[1]),
                            parts[2],
                            java.time.LocalDateTime.now().toString()
                    );

                    System.out.println("Recebido: " + line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
