package cl.iplacex.tiendaweb.service;

import com.google.gson.Gson;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FacturacionAdapter {

    private final Gson gson = new Gson();

    /**
     * Adapter de Facturación: Escucha el canal central (pedidos) y consume el servicio SOAP legado.
     */
    @JmsListener(destination = "pedidos")
    public void procesarFacturacion(String jsonMessage) {
        System.out.println("FacturacionAdapter recibido: " + jsonMessage);
        try {
            // 1. Recibir mensaje en formato canónico (JSON)
            Map<String, Object> pedido = gson.fromJson(jsonMessage, Map.class);

            // 2. Extraer información necesaria
            // Asumimos que el pedido tiene id, total, cliente, etc.
            Object idPedido = pedido.get("id");
            Object total = pedido.get("total");
            Object cliente = pedido.get("cliente");

            // 3. Consumir servicio web SOAP de Facturación
            emitirFacturaSoap(idPedido, cliente, total);

        } catch (Exception e) {
            System.err.println("Error en FacturacionAdapter: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void emitirFacturaSoap(Object idPedido, Object cliente, Object total) {
        // Simulación de la llamada al servicio SOAP legado
        System.out.println("--- INICIO LLAMADA SOAP FACTURACION ---");
        System.out.println("Enviando XML a servicio de facturación...");
        System.out.println("Cliente: " + cliente);
        System.out.println("Monto: " + total);
        System.out.println("Ref Pedido: " + idPedido);
        System.out.println("--- FIN LLAMADA SOAP FACTURACION ---");
    }
}
