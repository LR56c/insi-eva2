package cl.iplacex.tiendaweb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

@Service
public class TraductorPedidos {

    private final JmsTemplate jmsTemplate;
    private final Gson gson;

    @Autowired
    public TraductorPedidos(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
        this.gson = new Gson();
    }

    /**
     * Traductor Web: Convierte XML de la tienda al JSON Canónico.
     * Escucha en el canal: web_pedidos
     */
    @JmsListener(destination = "web_pedidos")
    public void traducirDesdeWeb(String xmlMessage) {
        System.out.println("Recibido en web_pedidos (XML): " + xmlMessage);
        try {
            // 1. Parsear el XML
            Map<String, Object> pedidoCanonico = parsearXmlAMapa(xmlMessage);
            
            // 2. Enriquecer o normalizar datos si es necesario (ej: agregar origen)
            pedidoCanonico.put("origen", "TIENDA_WEB");

            // 3. Enviar al canal unificado
            enviarAlCanalUnificado(pedidoCanonico);

        } catch (Exception e) {
            System.err.println("Error traduciendo mensaje Web: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Traductor Marketplace: Convierte JSON específico del marketplace al JSON Canónico.
     * Escucha en el canal: mkp_pedidos
     */
    @JmsListener(destination = "mkp_pedidos")
    public void traducirDesdeMarketplace(String jsonMessage) {
        System.out.println("Recibido en mkp_pedidos (JSON): " + jsonMessage);
        try {
            // 1. Parsear el JSON específico
            Map<String, Object> pedidoMkp = gson.fromJson(jsonMessage, Map.class);
            
            // 2. Transformar al Modelo Canónico
            // Aquí se realizaría el mapeo de campos si los nombres difieren.
            // Por ejemplo, si el MKP usa "shippingCost" y el Canónico usa "costo_envio"
            Map<String, Object> pedidoCanonico = new HashMap<>(pedidoMkp);
            
            if (pedidoCanonico.containsKey("shippingCost")) {
                pedidoCanonico.put("costo_envio", pedidoCanonico.remove("shippingCost"));
            }
            pedidoCanonico.put("origen", "MARKETPLACE");

            // 3. Enviar al canal unificado
            enviarAlCanalUnificado(pedidoCanonico);

        } catch (Exception e) {
            System.err.println("Error traduciendo mensaje Marketplace: " + e.getMessage());
        }
    }

    private void enviarAlCanalUnificado(Map<String, Object> pedido) {
        String jsonCanonico = gson.toJson(pedido);
        jmsTemplate.convertAndSend("pedidos", jsonCanonico);
        System.out.println("Mensaje traducido enviado al canal 'pedidos': " + jsonCanonico);
    }

    // Método auxiliar simple para parsear XML a Map (Simulación de transformación)
    private Map<String, Object> parsearXmlAMapa(String xml) throws Exception {
        Map<String, Object> map = new HashMap<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        
        Element root = doc.getDocumentElement();
        
        // Ejemplo básico de extracción de campos del XML
        // Asumiendo estructura <pedido><id>1</id><total>1000</total>...</pedido>
        if (root.getElementsByTagName("id").getLength() > 0) {
            map.put("id", root.getElementsByTagName("id").item(0).getTextContent());
        }
        if (root.getElementsByTagName("total").getLength() > 0) {
            map.put("total", root.getElementsByTagName("total").item(0).getTextContent());
        }
        if (root.getElementsByTagName("cliente").getLength() > 0) {
            map.put("cliente", root.getElementsByTagName("cliente").item(0).getTextContent());
        }
        
        return map;
    }
}
