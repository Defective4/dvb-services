package io.github.defective4.tv.dvbservices.util;

import java.util.function.Consumer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DOMUtils {
    public static final DocumentBuilder DOC_BUILDER;
    public static final Transformer XML_TRANSFORMER;
    public static final Transformer XMLTV_TRANSFORMER;

    static {
        try {
            DOC_BUILDER = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder();
            XMLTV_TRANSFORMER = TransformerFactory.newDefaultInstance().newTransformer();
            XMLTV_TRANSFORMER.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            XMLTV_TRANSFORMER.setOutputProperty(OutputKeys.INDENT, "yes");
            XML_TRANSFORMER = TransformerFactory.newDefaultInstance().newTransformer();
            XML_TRANSFORMER.setOutputProperty(OutputKeys.INDENT, "yes");
        } catch (ParserConfigurationException | TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private DOMUtils() {}

    public static void createAndAppendElement(Node parent, String tag, Consumer<Element> consumer) {
        Element element = (parent instanceof Document doc ? doc : parent.getOwnerDocument()).createElement(tag);
        consumer.accept(element);
        parent.appendChild(element);
    }
}
