package cl.iplacex.tiendaweb.service;

import cl.iplacex.tiendaweb.JmsConfig;
import cl.iplacex.tiendaweb.domain.Orden;
import cl.iplacex.tiendaweb.ext.carrito.domain.Direccion;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.jms.*;
import org.springframework.stereotype.Service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class MarketplaceTranslator {

    private final ConnectionFactory connectionFactory;
    private final Gson gson = new Gson();

    private JMSContext context;
    private JMSProducer producer;
    private Destination targetDestination;

    public MarketplaceTranslator(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    public void init() {
        try {
            this.context = connectionFactory.createContext();
            this.targetDestination = context.createTopic(JmsConfig.COLA_PEDIDOS_CENTRAL);
            this.producer = context.createProducer();
            Destination mkpQueue = context.createQueue(JmsConfig.COLA_MKP_PEDIDOS);
            JMSConsumer mkpConsumer = context.createConsumer(mkpQueue);
            mkpConsumer.setMessageListener(this::onMessageMkp);
            Destination webQueue = context.createQueue(JmsConfig.COLA_WEB_PEDIDOS);
            JMSConsumer webConsumer = context.createConsumer(webQueue);
            webConsumer.setMessageListener(this::onMessageWeb);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void cleanup() {
        if (context != null) context.close();
    }

    public void onMessageMkp(Message message) {
        try {

            String rawJson = ((TextMessage) message).getText();
            System.out.println("Mensaje recibido de Marketplace: " + rawJson);

            JsonObject root = JsonParser.parseString(rawJson).getAsJsonObject();
            JsonObject client = root.getAsJsonObject("cliente");

            double totalAmount = root.get("montoTotal").getAsDouble();
            if (root.has("costoEnvio") && !root.get("costoEnvio").isJsonNull()) {
                totalAmount += root.get("costoEnvio").getAsDouble();
            }

            Orden pedido = new Orden();
            pedido.setOrigen("marketplace");
            if (root.has("id")) {
                pedido.setId(root.get("id").getAsString());
            }
            if (root.has("fecha")) {
                pedido.setFecha(root.get("fecha").getAsString());
            }
            pedido.setRut(client.get("rut").getAsString());
            pedido.setClientName(client.get("nombre").getAsString() + " " + client.get("apellido").getAsString());
            pedido.setTotal(totalAmount);

            if (root.has("direccion")) {
                JsonObject dirJson = root.getAsJsonObject("direccion");
                Direccion dir = new Direccion();
                String calle = dirJson.has("calle") ? dirJson.get("calle").getAsString() : "";
                String numero = dirJson.has("numero") ? dirJson.get("numero").getAsString() : "";
                dir.setCalleYNumero((calle + " " + numero).trim());
                if (dirJson.has("comuna")) dir.setComuna(dirJson.get("comuna").getAsString());
                pedido.setDireccion(dir);
            }

            if (root.has("items")) {
                List<Orden.Item> items = new ArrayList<>();
                JsonArray itemsJson = root.getAsJsonArray("items");
                for (JsonElement el : itemsJson) {
                    JsonObject itemObj = el.getAsJsonObject();
                    String prod = itemObj.has("producto") ? itemObj.get("producto").getAsString() : "";
                    int cant = itemObj.has("cantidad") ? itemObj.get("cantidad").getAsInt() : 0;
                    double prec = itemObj.has("precioUnitario") ? itemObj.get("precioUnitario").getAsDouble() : 0;
                    items.add(new Orden.Item(prod, cant, prec));
                }
                pedido.setItems(items);
            }

            String jsonCanonico = gson.toJson(pedido);
            System.out.println("Mensaje Canónico enviado: " + jsonCanonico);
            producer.send(targetDestination, jsonCanonico);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void onMessageWeb(Message message) {
        try {
            String xmlWeb = ((TextMessage) message).getText();
            System.out.println("Mensaje recibido de Web: " + xmlWeb);
            Orden pedido = parsearXml(xmlWeb);
            String jsonCanonico = gson.toJson(pedido);
            System.out.println("Mensaje Canónico enviado: " + jsonCanonico);
            producer.send(targetDestination, jsonCanonico);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Orden parsearXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        Element root = doc.getDocumentElement();
        
        Orden pedido = new Orden();
        pedido.setOrigen("web");
        pedido.setFecha(getTagValue("fecha", root));

        Element comprador = (Element) root.getElementsByTagName("comprador").item(0);
        String nombre = getTagValue("nombre", comprador) + " " + getTagValue("apellido", comprador);
        pedido.setClientName(nombre.trim());
        pedido.setRut(getTagValue("rut", comprador));

        Element dirEl = (Element) root.getElementsByTagName("direccionDespacho").item(0);
        if (dirEl != null) {
            Direccion dir = new Direccion();
            dir.setCalleYNumero(getTagValue("calleYNumero", dirEl));
            dir.setComuna(getTagValue("comuna", dirEl));
            pedido.setDireccion(dir);
        }

        double total = 0;
        List<Orden.Item> itemsList = new ArrayList<>();
        NodeList items = root.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            Element producto = (Element) item.getElementsByTagName("producto").item(0);
            
            String nombreProd = getTagValue("nombre", producto);
            String precioStr = getTagValue("precioLista", producto);
            double precio = precioStr.isEmpty() ? 0 : Double.parseDouble(precioStr);
            
            String cantidadStr = getTagValue("cantidad", item);
            int cantidad = cantidadStr.isEmpty() ? 0 : Integer.parseInt(cantidadStr);
            
            total += precio * cantidad;
            itemsList.add(new Orden.Item(nombreProd, cantidad, precio));
        }

        pedido.setTotal(total);
        pedido.setItems(itemsList);
        return pedido;
    }

    private String getTagValue(String tag, Element element) {
        if (element == null) return "";
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }
}
