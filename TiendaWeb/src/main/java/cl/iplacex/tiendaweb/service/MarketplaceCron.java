package cl.iplacex.tiendaweb.service;

import cl.iplacex.tiendaweb.JmsConfig;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.gson.Gson;

import java.util.Map;

@Service
public class MarketplaceCron {

    private final RestTemplate restTemplate;
    private final JmsTemplate jmsTemplate;
    private final Gson gson;

    @Autowired
    public MarketplaceCron(JmsTemplate jmsTemplate) {
        this.restTemplate = new RestTemplate();
        this.jmsTemplate = jmsTemplate;
        this.gson = new Gson();
    }

        @Scheduled(fixedRate = 1000000)
//    @Scheduled(cron = "0 0 8 * * *")
    public void procesarPedidosDiarios() {
        try {
            String ordersUrl = "http://localhost:8091/orders/today";
            Map[] orders = restTemplate.getForObject(ordersUrl, Map[].class);

            if (orders == null) {
                return;
            }
            for (Map order : orders) {
                try {
                    Object idObj = order.get("id");
                    if (idObj == null) {
                        continue;
                    }
                    String orderId = idObj.toString();
                    String shippingCostUrl = "http://localhost:8091/orders/" + orderId + "/shipping-cost";
                    Map shippingInfo = restTemplate.getForObject(shippingCostUrl, Map.class);
                    if (shippingInfo == null || !shippingInfo.containsKey("costoEnvio")) {
                        continue;
                    }

                    order.put("costoEnvio", shippingInfo.get("costoEnvio"));
                    String jsonMessage = gson.toJson(order);
                    System.out.println("Mensaje enviado desde MarketplaceCron: " + jsonMessage);

                    jmsTemplate.convertAndSend(JmsConfig.COLA_MKP_PEDIDOS, jsonMessage);
                } catch (Exception e) {
                    System.err.println("Error procesando pedido individual: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error al obtener pedidos del marketplace: " + e.getMessage());
        }
    }
}
