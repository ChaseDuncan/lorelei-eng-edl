/**
 * 
 */
package edu.illinois.cs.cogcomp.lorelei.xml;

import edu.illinois.cs.cogcomp.lorelei.xml.XMLException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

/**
 * @author Eric Bengtson
 * 
 */
public class SimpleXMLParser {

	/**
	 * @param file
	 *                The text of the file to parse.
	 * @param dtdpath The path to the dtd files
	 *
	 * @return The {@code Document} XML tag -- the root of the
	 *         document.
	 * @throws XMLException
	 */
	public static Document getDocument(InputStream file, final String dtdpath) throws XMLException {

		DocumentBuilder docBuilder;
		if(dtdpath != "") {
			docBuilder = getDocumentBuilder();
			docBuilder.setEntityResolver((publicId, systemId) -> {

				File f = new File(systemId);
				String fname = f.getName();

				if (systemId.contains("laf") || systemId.contains("ltf")) {
					return new InputSource(new FileReader(dtdpath + fname));
				} else {
					return null;
				}
			});
		}else{
			// don't validate.
			docBuilder = getDocumentBuilder(false);
			docBuilder.setEntityResolver((publicId, systemId) -> {
				System.out.println("Ignoring " + publicId + ", " + systemId);
				return new InputSource(new StringReader(""));
			});
		}

		return getDocument(file, docBuilder);
	}


	/**
	 * @param file
	 *                The file to parse.
	 * @param dtdpath The path to the dtd files
	 *
	 * @return The {@code Document} XML tag -- the root of the
	 *         document.
	 * @throws XMLException
	 */
	public static Document getDocument(File file, final String dtdpath) throws XMLException {

		DocumentBuilder docBuilder;
		if(dtdpath != "") {
			docBuilder = getDocumentBuilder();
			docBuilder.setEntityResolver((publicId, systemId) -> {

				File f = new File(systemId);
				String fname = f.getName();

				if (systemId.contains("laf") || systemId.contains("ltf")) {
					return new InputSource(new FileReader(dtdpath + fname));
				} else {
					return null;
				}
			});
		}else{
			// don't validate.
			docBuilder = getDocumentBuilder(false);
			docBuilder.setEntityResolver((publicId, systemId) -> {
				System.out.println("Ignoring " + publicId + ", " + systemId);
				return new InputSource(new StringReader(""));
			});
		}


		return getDocument(file, docBuilder);
	}

	/**
	 * @param file
	 *                The file to parse.
	 * @return The {@code Document} XML tag -- the root of the
	 *         document.
	 * @throws XMLException
	 */
	public static Document getDocument(File file) throws XMLException {
		return getDocument(file, "");
	}

    /**
     * @param parent
     *                The node containing the desired element tag
     * @param tagName
     *                The name of the tag
     * @return the first target element inside the parent Node.
     * @throws XMLException
     */
    public static Element getElement(Element parent, String tagName)
    throws XMLException {
	NodeList nl = parent.getElementsByTagName(tagName);
	if (nl.getLength() <= 0)
	    throw new XMLException("Element Not Found");
	return (Element) nl.item(0);
    }

    /**
     * @param tag
     *                (start) tag that encloses desired text.
     * @return The contents of the tag.
     * @throws XMLException If the tag has no children.
     */
    public static String getContentString(Element tag) throws XMLException {
	if (!tag.hasChildNodes())
	    throw new XMLException("tag " + tag.getNodeName() + " empty");
	return tag.getFirstChild().getNodeValue();
    }

    /**
     * @param parent
     *                A node containing the tag enclosing desired text.
     * @param tagName
     *                Name of tag containing desired text.
     * @return The desired text.
     * @throws XMLException
     */
    public static String getTagContent(Element parent, String tagName)
    throws XMLException {
	return getContentString(getElement(parent, tagName));
    }

    /**
     * @param parent
     *                A node containing the tag enclosing desired text.
     * @param tagName
     *                Name of tag containing desired text.
     * @return The desired text, or if tag not found, defaultResult.
     */
    public static String getTagContent(Element parent, String tagName,
                                       String defaultResult) {
	NodeList nl = parent.getElementsByTagName(tagName);
	if (nl.getLength() <= 0)
	    return defaultResult;
	Element tag = (Element) nl.item(0);
	if (!tag.hasChildNodes())
	    return defaultResult;
	return tag.getFirstChild().getNodeValue();
    }

    /** ** UTILITY METHODS *** */

	private static DocumentBuilder getDocumentBuilder(boolean validate) throws XMLException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(validate);

		try {
			return factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new XMLException("Misconfigured XML Parser", e);
		}
	}

    private static DocumentBuilder getDocumentBuilder() throws XMLException {
		return getDocumentBuilder(true);
    }

	private static Document getDocument(InputStream file, DocumentBuilder docBuilder)
			throws XMLException {
		try {
			//System.out.println("IS VALIDATING: " + docBuilder.isValidating());
			return docBuilder.parse(file);
		} catch (SAXException e) {
			throw new XMLException("Parse Error Occurred", e);
		} catch (IOException e) {
			throw new XMLException("IO Error While Reading XML Document", e);
		}
	}


	private static Document getDocument(File file, DocumentBuilder docBuilder)
			throws XMLException {
		try {
			//System.out.println("IS VALIDATING: " + docBuilder.isValidating());
			return docBuilder.parse(file);
		} catch (SAXException e) {
			throw new XMLException("Parse Error Occurred", e);
		} catch (IOException e) {
			throw new XMLException("IO Error While Reading XML Document", e);
		}
	}

}
