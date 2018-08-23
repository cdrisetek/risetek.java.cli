package risetek.jcli.ext;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import risetek.jcli.Cli_common;

public class XMLParser {
	private final DocumentBuilderFactory factory;
	private DocumentBuilder builder = null;
    public XMLParser() {
    	factory = DocumentBuilderFactory.newInstance();
        try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
    }
    
    public void parser(String xml) throws SAXException, IOException{
		Document document = builder.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
		NodeList nodeList = document.getElementsByTagName("modem");
		for(int i=0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			NamedNodeMap map = node.getAttributes();
			for(int n=0;n<map.getLength();n++) {
				Node attr = map.item(n);
				Cli_common.Debug(1, attr.getNodeName() + " value is: " + attr.getNodeValue());
			}
		}
    }
}
