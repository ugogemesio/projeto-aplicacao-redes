# Projeto de Aplicação de REDES – Automação com ESP32 e Java

Sistema de monitoramento e automação baseado em ESP32 para coleta de dados e tomada de decisão, com Java + Spring Boot como backend e uma interface simples via Thymeleaf.
Este projeto demonstra integração de dispositivos IoT com serviços backend, cálculos estatísticos, comunicação em tempo real e armazenamento persistente.
---

## Índice

1. [Objetivo](#objetivo)
2. [Funcionalidades](#funcionalidades)
3. [Arquitetura do Sistema](#arquitetura-do-sistema)
4. [Fluxo de Uso](#fluxo-de-uso)
5. [Execução do Projeto](#execucao-do-projeto)
6. [Performance](#performance)
7. [Limitações Conhecidas](#limitacoes-conhecidas)
8. [Contribuição](#contribuicao)

---

## Objetivo

Criar uma solução para gerenciar de modo controlável e escalável ESP32 via backend JAVA 

- O ESP32 coleta dados de sensores, executa decisões programáveis e mantém uma interface de usuário local (display, WebServer, etc.).
- O Backend em Java (Spring Boot) recebe dados dos ESP32, realiza cálculos como métricas de rede (RTT, jitter, throughput), persiste dados e envia atualizações ao frontend. 
- O Frontend em Thymeleaf exibe valores em tempo real e permite ilustrar o conceito de integração IoT + backend.

A aplicação busca demonstrar aspectos fundamentais de Redes de Computadores, como:
- comunicação TCP/WebSocket,
- ACKs,
- latência,
- estatísticas de transferência,
- consistência de tempo,
- e persistência de dados.
---
## Funcionalidades

ESP32
- Envio periódico de dados (sensores, métricas, status, etc.) via TCP ou WebSocket.
 Realiza tomadas de decisão locais (ex.: acionar atuadores).
- Pode gerar uma interface básica local (OLED, serial, webserver).
- Reenvio automático em caso de falha de comunicação.
- Cálculo local de timestamps para RTT quando requerido.

Backend Java + Spring Boot
- Recebe dados dos ESP32 via socket/TCP.
- Processa métricas:
  - RTT
  - Jitter
  - Throughput
- Envia dados ao frontend em tempo real (STOMP WebSocket).
- Persistência em banco (in-memory, PostgreSQL ou outro).
- API REST opcional para consultas externas.

Frontend Thymeleaf
- Dashboard com gráficos em tempo real.
- Visualização de pacotes recebidos, métricas e status dos dispositivos.
- Interface simples para fins educacionais e demonstração.
---
## Arquitetura do Sistema

Visão geral
```bash
ESP32  ->  Backend Spring Boot  ->  Frontend Thymeleaf
   |              |                       |
Sensores      Processamento            Visualização
Atores        Persistência           em tempo real
```
## Componentes
| Componente      | Função                                                             |
| --------------- | ------------------------------------------------------------------ |
| **ESP32 (C++)** | Coleta dados, envia via TCP/WebSocket, executa lógica local.       |
| **Spring Boot** | Servidor central, cálculo de métricas, persistência, websocket.    |
| **Database**    | (In-memory ou PostgreSQL) guarda histórico de leituras e métricas. |
| **Thymeleaf**   | Interface web com gráficos e dados atualizados.                    |

---

## Fluxo de Uso

1. ESP32 inicializa sensores e conexão.

2. Envia dados periódicos ao backend (timestamp, valores, ACK, sequência, etc.).

3. Backend processa dados, calcula métricas e armazena.

4. Backend envia atualizações ao frontend via WebSocket (ainda não implementado).

5. Usuário visualiza métricas em tempo real.

6. Usuário controla ESP32 via frontend (ainda não implementado).

---

## Execução do Projeto

**Pré-requisitos:** 
- Java 25
- PostgreSQL se quiser persistência,
-  Gradle,
-  ESP32
  - DHT11 - sensor de temperatura e umidade utilizado como exemplo. 
  - qualquer outro módulo que se queira controlar e interagir.

### Backend

1. Clone o repositório
2. Configure o application.properties (porta, banco, etc.)
3. Rode:


#### ESP32

1. Suba o código no microcontrolador (PlatformIO / Arduino IDE).

2. Configure SSID, senha e IP do servidor Java.

3. Flash no dispositivo.

---

##  Performance

Performance


- Throughput médio

- RTT médio

- Jitter

- Taxa de pacotes perdidos

Testes de carga 

# Exemplo de registro
Throughput: 15.3 bytes/s
RTT: 132 ms
Jitter: 8 ms
Pacotes recebidos: 152

---

## Limitações Conhecidas

---

## Contribuição

1. Faça **fork** do repositório.
2. Implemente alterações.
3. Crie **Pull Request** para análise.
