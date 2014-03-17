package fr.lipn.so.indexing;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import fr.lipn.so.common.IR2;

public class SecondOrderXMLHandler extends DefaultHandler {
	  
	 /* A buffer for each XML element */
	  protected StringBuffer textBuffer = new StringBuffer();
	  protected StringBuffer titleBuffer = new StringBuffer();
	  protected StringBuffer docBuffer = new StringBuffer();
	  protected String docID = new String();
	  
	  protected Stack<String> elemStack;
	  protected Document currentDocument;
	  protected Vector<Document> parsedDocuments;
	  
	  //used to retrieve the second-order values
	  private IndexReader reader;
	  private IndexSearcher searcher;
	  private Analyzer analyzer;
	  
	  //the retrieved IDs set for each document
	  private HashSet<String> ids;
	  
	  //the sentence representation using id:score pairs
	  private StringBuffer sentIdScores= new StringBuffer();
	  private final static int K=70; //parameter for the comparison 
	  
	  
	  public SecondOrderXMLHandler(File xmlFile, String index) 
	  	throws ParserConfigurationException, SAXException, IOException {
	    
		reader = IndexReader.open(FSDirectory.open(new File(index)));
		searcher = new IndexSearcher(reader);
		searcher.setSimilarity(new BM25Similarity());
		analyzer = new EnglishAnalyzer(Version.LUCENE_44);
		  
		// Now let's move to the parsing stuff
	    SAXParserFactory spf = SAXParserFactory.newInstance();
	    SAXParser parser = spf.newSAXParser();
	    this.docID=xmlFile.getName();
	    
	    try {
	      parser.parse(xmlFile, this);
	    } catch (org.xml.sax.SAXParseException spe) {
	      System.out.println("SAXParser caught SAXParseException at line: " +
	        spe.getLineNumber() + " column " +
	        spe.getColumnNumber() + " details: " +
			spe.getMessage());
	    }
	  }

	  // call at document start
	  public void startDocument() throws SAXException {
		  parsedDocuments= new Vector<Document>();
		  elemStack=new Stack<String>();
	  }

	  // call at element start
	  public void startElement(String namespaceURI, String localName,
	    String qualifiedName, Attributes attrs) throws SAXException {

	    String eName = localName;
	     if ("".equals(eName)) {
	       eName = qualifiedName; // namespaceAware = false
	     }
	     
	     elemStack.addElement(eName);
	     if(eName=="DOC") {
	     	textBuffer.setLength(0);
	     	titleBuffer.setLength(0);
	     	docBuffer.setLength(0);
	     	
	     	ids=new HashSet<String>();
	     	sentIdScores.setLength(0);
	     	
	     	currentDocument=new Document();
	     }

	  }

	  // call when cdata found
	  public void characters(char[] text, int start, int length)
	    throws SAXException {
	  	if(elemStack.peek().equalsIgnoreCase("HEADLINE")){
	  		titleBuffer.append(text, start, length);
	  	} else if (elemStack.peek().equalsIgnoreCase("TEXT")) {
	  		textBuffer.append(text, start, length);
	  	} else if (elemStack.peek().equalsIgnoreCase("DOCNO")) {
	  		docBuffer.append(text,start,length);
	  	}
	  }

	  // call at element end
	  public void endElement(String namespaceURI, String simpleName,
	    String qualifiedName)  throws SAXException {

	    String eName = simpleName;
	    if ("".equals(eName)) {
	      eName = qualifiedName; // namespaceAware = false
	    }
	    elemStack.pop();
	    
	    if(eName.equals("DOCNO")){
	    	this.docID=docBuffer.toString();
	    }
	    
	    if (eName.equals("DOC")){
	    	//standard indexing stuff
	    	currentDocument.add(new Field("title", titleBuffer.toString(), Field.Store.YES, Field.Index.ANALYZED));
	    	currentDocument.add(new Field("text", textBuffer.toString(), Field.Store.YES, Field.Index.ANALYZED)); 
	    	currentDocument.add(new Field("id", this.docID, Field.Store.YES, Field.Index.NOT_ANALYZED));
	    	
	    	//indirect indexing stuff
	    	Vector<String> sentences = getSentences(titleBuffer.toString()+". "+textBuffer.toString());
	    	for(String s : sentences) setSimilarityDataForSentence(s);
	    	currentDocument.add(new Field("sentscores", sentIdScores.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS)); //we just store it
	    	
	    	StringBuffer allIDs= new StringBuffer();
	    	for(String id : ids) allIDs.append(id+" ");
	    	currentDocument.add(new Field("docs", allIDs.toString(), Field.Store.YES, Field.Index.ANALYZED)); //analysis is required since it needs to split on spaces
	    	
	    	//now add the document to the parsed ones
	    	parsedDocuments.add(currentDocument);
	    	
	    	System.err.print(".");
	    }
	    
	    if (eName.equals("DOCS")){
	    	try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	    	System.err.println();
	    }
	  }
	  
	  public Vector<Document> getParsedDocuments() {
		  return this.parsedDocuments;
	  }
	  
	  private Vector<String> getSentences(String text){
		  Vector<String> sentenceList = new Vector<String>();
		  DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(text));
		  
		  Iterator<List<HasWord>> it = dp.iterator();
		  //int nSentences=0;
		  while (it.hasNext()) {
			 /*
			 if(IR2.SENTENCE_LIMIT>0){
				 if (nSentences > IR2.SENTENCE_LIMIT) break;
			 }
			 */
		     StringBuilder sentenceSb = new StringBuilder();
		     List<HasWord> sentence = it.next();
		     for (HasWord token : sentence) {
		        if(sentenceSb.length()>1) {
		           sentenceSb.append(" ");
		        }
		        sentenceSb.append(token);
		     }
		     sentenceList.add(sentenceSb.toString());
		     //nSentences++;
		  }
		  /*
		  for(String sentence:sentenceList) {
		     System.err.println(sentence);
		  }
		  */
		  return sentenceList;
		  
	  }
	  
	  private void setSimilarityDataForSentence(String sentence){
		  try {
			  QueryParser parser = new QueryParser(Version.LUCENE_44, IR2.RELEVANT_FIELD, analyzer);
			  sentence=sentence.replaceAll( "[^\\w]", " ");
			  if(sentence.trim().length()>0) {
				  Query query = parser.parse(sentence.toLowerCase().trim());
				  
				  //System.err.println("searching sentence: "+query.toString());
				  
				  TopDocs results = searcher.search(query, K);
				  ScoreDoc[] hits = results.scoreDocs;
				  
				  int n = results.totalHits;
				  
				  for (int i = 0; i < Math.min(K, n); i++) {
			    	Document doc = searcher.doc(hits[i].doc);
				    String id = doc.get("id");
				    //id=id.replaceAll("[\\W]", "_");
				    Float score = new Float(hits[i].score);
				    //System.err.println("found doc: "+id+" : "+score);
				    ids.add(id);
				    sentIdScores.append(id+":"+score);
				    sentIdScores.append(" ");
			      }
				  
				  sentIdScores.append("|");
				  //System.err.println("sentIdScores: "+sentIdScores);
			  }
		  
		  } catch (Exception e) {
		      e.printStackTrace();
		  }
		  
	  }
}
