package fr.lipn.so.common;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import fr.lipn.so.indexing.SecondOrderXMLHandler;

public class SecondIndexer {
	public static LexicalizedParser parser;
	
	private static String firstIndex = "/tempo/corpora/AQUAINT_indexed";
	//private static String firstIndex = "/tempo/corpora/DBPedia/indexed";
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		parser = LexicalizedParser.loadModel("lib/englishPCFG.ser.gz");
		//String indexPath = "/tempo/corpora/secondIndexWiki"; //where to store the index
		String indexPath = "/tempo/corpora/secondIndex"; //where to store the index
		String docsPath = "/tempo/corpora/CLEF/english"; //documents to analyze
		
		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
		   System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
		   System.exit(1);
		}
		   
		Date start = new Date();
		try {
		     System.out.println("Indexing to directory '" + indexPath + "'...");

		     Directory dir = FSDirectory.open(new File(indexPath));
		     /*Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();
			 analyzerPerField.put("title", new EnglishAnalyzer(Version.LUCENE_44));
			 analyzerPerField.put("text", new EnglishAnalyzer(Version.LUCENE_44));
			 analyzerPerField.put("docs", new EnglishAnalyzer(Version.LUCENE_44));
			 	
			 PerFieldAnalyzerWrapper analyzer =
			    	      new PerFieldAnalyzerWrapper(new EnglishAnalyzer(Version.LUCENE_44), analyzerPerField);
			   */
		     Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_44);
		     
			 IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44, analyzer);
			 iwc.setSimilarity(new BM25Similarity());
		     iwc.setOpenMode(OpenMode.CREATE);
			    
		     IndexWriter writer = new IndexWriter(dir, iwc);
		     indexDocs(writer, docDir);

		     writer.close();

		     Date end = new Date();
		     System.out.println(end.getTime() - start.getTime() + " total milliseconds");

		   } catch (IOException e) {
		     System.out.println(" caught a " + e.getClass() +
		      "\n with message: " + e.getMessage());
		   }
		 }

		 /**
		  * Indexes the given file using the given writer, or if a directory is given,
		  * recurses over files and directories found under the given directory.
		  *  
		  * @param writer Writer to the index where the given file/dir info will be stored
		  * @param file The file to index, or the directory to recurse into to find files to index
		  * @throws IOException
		  */
		 static void indexDocs(IndexWriter writer, File file)
		   throws IOException {
		   // do not try to index files that cannot be read
		   if (file.canRead()) {
		     if (file.isDirectory()) {
		       String[] files = file.list();
		       // an IO error could occur
		       if (files != null) {
		         for (String f : files) {
		           indexDocs(writer, new File(file, f));
		         }
		       }
		     } else {
		   	if(file.getName().endsWith(".xml")) {
		   		System.out.println("indexing " + file);
		           try {
		           		SecondOrderXMLHandler hdlr = new SecondOrderXMLHandler(file, firstIndex);
		           		Vector<Document> docs=hdlr.getParsedDocuments();
		           		for(Document doc : docs) writer.addDocument(doc);
		           } catch (Exception e) {
		           	e.printStackTrace();
		           } 
			      }
			    }
		   }

	  }
}


