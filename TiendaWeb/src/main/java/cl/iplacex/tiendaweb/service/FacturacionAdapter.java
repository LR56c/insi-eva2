package cl.iplacex.tiendaweb.service;

import cl.iplacex.tiendaweb.JmsConfig;
import cl.iplacex.tiendaweb.domain.Orden;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.jms.*;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class FacturacionAdapter implements MessageListener {

    private final ConnectionFactory connectionFactory;
    private final Gson gson = new Gson();
    private JMSContext context;


    public FacturacionAdapter(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    public void init() {
        try {
            this.context = connectionFactory.createContext();
            JMSConsumer consumer = context.createConsumer(context.createQueue(JmsConfig.COLA_PEDIDOS_FACTURACION));
            consumer.setMessageListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void cleanup() {
        if (context != null) context.close();
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage)) {
                return;
            }
            String jsonMessage = ((TextMessage) message).getText();
            System.out.println("Mensaje recibido en FacturacionAdapter: " + jsonMessage);

            Orden pedido = gson.fromJson(jsonMessage, Orden.class);

            enviarSoap(pedido);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private void enviarSoap(Orden pedido) {
        try {
            String soapBody = String.format(
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:exa=\"http://example.org/\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <exa:generarBoleta>\n" +
                    "         <cliente>%s</cliente>\n" +
                    "         <rut>%s</rut>\n" +
                    "         <total>%d</total>\n" +
                    "      </exa:generarBoleta>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>",
                    pedido.getClientName(),
                    pedido.getRut(),
                    (long) pedido.getTotal()
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8090/soap/facturacion"))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(soapBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Respuesta SOAP: " + response.body());
        } catch (Exception e) {
            System.err.println("Error al enviar solicitud SOAP: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
