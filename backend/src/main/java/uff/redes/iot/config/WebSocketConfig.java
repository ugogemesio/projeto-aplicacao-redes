package uff.redes.iot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;


//@Configuration → diz ao Spring que esta classe contém configurações e beans.
//
//@EnableWebSocketMessageBroker → ativa o suporte a WebSocket com STOMP no Spring.
//
//  Ou seja, permite que sua aplicação receba e
//      envie mensagens via WebSocket usando tópicos e filas STOMP.
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {


//    addEndpoint("/ws") → define o endpoint que os clientes usarão para se conectar via WebSocket.
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

//    Mensagens do servidor para clientes → /topic/...
//    Mensagens do cliente para servidor → /app/...
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
