package fr.lipn.so.search;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TopicHandler extends DefaultHandler {
	private Vector<RobustCLIRQuery> queries;
	private Stack<String> elemStack;
	
	protected StringBuffer narrBuffer = new StringBuffer();
	protected StringBuffer titleBuffer = new StringBuffer();
	protected StringBuffer descBuffer = new StringBuffer();
	protected StringBuffer docBuffer = new StringBuffer();
	
	public TopicHandler(String queryFile) {
		queries= new Vector<RobustCLIRQuery>();
		
		File xmlFile = new File(queryFile);
		SAXParserFactory spf = SAXParserFactory.newInstance();
	    try {
			SAXParser parser = spf.newSAXParser();
			parser.parse(xmlFile, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void startDocument() throws SAXException {
		  elemStack=new Stack<String>();
	}

	public void startElement(String namespaceURI, String localName,
	    String qualifiedName, Attributes attrs) throws SAXException {

	    String eName = localName;
	    if ("".equals(eName)) {
	      eName = qualifiedName; // namespaceAware = false
	    }
	     
	   elemStack.addElement(eName);
	   if(eName=="topic") {
		   narrBuffer.setLength(0);
	   	   titleBuffer.setLength(0);
	   	   docBuffer.setLength(0);
	   	   descBuffer.setLength(0);

	     }

	  }

	  // call when cdata found
	  public void characters(char[] text, int start, int length)
	    throws SAXException {
	  	if(elemStack.peek().equalsIgnoreCase("id")){
	  		docBuffer.append(text, start, length);
	  	} else if (elemStack.peek().equalsIgnoreCase("title")) {
	  		titleBuffer.append(text, start, length);
	  	} else if (elemStack.peek().equalsIgnoreCase("desc")) {
	  		descBuffer.append(text,start,length);
	  	} else if (elemStack.peek().equalsIgnoreCase("narr")) {
	  		narrBuffer.append(text,start,length);
	  	}
	  }
	  
	  public void endElement(String namespaceURI, String simpleName,
		  String qualifiedName)  throws SAXException {

		    String eName = simpleName;
		    if ("".equals(eName)) {
		      eName = qualifiedName; // namespaceAware = false
		    }
		    elemStack.pop();
			    
		    if(eName.equals("topic")){
		    	this.queries.add(new RobustCLIRQuery(docBuffer.toString(), titleBuffer.toString(), descBuffer.toString(), narrBuffer.toString()));
		    }
			    
	  }
	  
	public Vector<RobustCLIRQuery> getParsedQueries() {
		return queries;
	}
}
