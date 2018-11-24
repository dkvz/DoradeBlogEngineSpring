package eu.dkvz.utils;

import org.w3c.dom.*;

public class XMLUtils {
	
	public static void addTextElement(Document doc, Element root, String name, String text) {
		Element el = doc.createElement(name);
        el.setTextContent(text);
        root.appendChild(el);
	}
	
}
