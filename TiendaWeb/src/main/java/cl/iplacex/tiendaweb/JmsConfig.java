package cl.iplacex.tiendaweb;


import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JmsConfig {

    public static final String COLA_MKP_PEDIDOS = "mhe_mkp_pedidos";
    public static final String COLA_WEB_PEDIDOS = "mhe_web_pedidos";
    public static final String COLA_PEDIDOS_CENTRAL = "mhe_pedidos";
    public static final String COLA_PEDIDOS_FACTURACION = "mhe_pedidos_facturacion";

    @Bean
    public ConnectionFactory connectionFactory() {
        try {
            return new ActiveMQConnectionFactory("tcp://localhost:61616", "admin", "admin");
        } catch (Exception e) {
            throw new RuntimeException("Error configurando JMS Factory", e);
        }
    }
}