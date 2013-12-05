package fr.lipn.so.search;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import fr.lipn.so.indexing.BooleanSimilarity;
import fr.lipn.so.ranking.WeightedDocument;

public class SORobustCLIR {
	static boolean CLASSIC=false;
	private static int K=70;
	private static int MAX_DOCS=1000;
	
	private final static int TITLE_ONLY=0;
	private final static int TITLE_DESC=1;
	private final static int TITLE_DESC_NARR=2;
	
	private static int MODE=TITLE_ONLY;
	
	private static Similarity baselinesimilarity=new BM25Similarity(); //DefaultSimilarity() or BM25Similarity()
	
	
	public static void main(String[] args) throws IOException, ParseException {
		String fIndex = "/tempo/corpora/AQUAINT_indexed";
		String sIndex = "/tempo/corpora/secondIndex";
		
		String field = "text";
		
		String queryFile = "/tempo/corpora/CLEF-wsd/topics/topics-en.xml";
		
		//FO reader
		IndexReader freader = IndexReader.open(FSDirectory.open(new File(fIndex)));
		IndexSearcher fsearcher = new IndexSearcher(freader);
		Analyzer fanalyzer = new EnglishAnalyzer(Version.LUCENE_44);
		IndexWriterConfig fiwc = new IndexWriterConfig(Version.LUCENE_44, fanalyzer);
		
		fiwc.setSimilarity(baselinesimilarity);

		//SO reader
		IndexReader sreader = IndexReader.open(FSDirectory.open(new File(sIndex)));
		IndexSearcher ssearcher = new IndexSearcher(sreader);
		
		Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();
		analyzerPerField.put("title", new EnglishAnalyzer(Version.LUCENE_44));
		analyzerPerField.put("text", new EnglishAnalyzer(Version.LUCENE_44));
		analyzerPerField.put("docs", new EnglishAnalyzer(Version.LUCENE_44));
		 	
		PerFieldAnalyzerWrapper sanalyzer =
		   	      new PerFieldAnalyzerWrapper(new EnglishAnalyzer(Version.LUCENE_44), analyzerPerField);
		    
		IndexWriterConfig siwc = new IndexWriterConfig(Version.LUCENE_44, sanalyzer);
		siwc.setSimilarity(new BooleanSimilarity());
		
		Vector<RobustCLIRQuery> queries = null;
		try {
       		TopicHandler hdlr = new TopicHandler(queryFile);
       		queries=hdlr.getParsedQueries();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		
		QueryParser parser = new QueryParser(Version.LUCENE_44, field, fanalyzer);
		
		
		for(RobustCLIRQuery rcq : queries) {
			String line=null;
			if(MODE==TITLE_ONLY) line = rcq.title;
			else if (MODE==TITLE_DESC) line = rcq.title+" "+rcq.description;
			else if (MODE==TITLE_DESC_NARR) line = rcq.title+" "+rcq.description+" "+rcq.narrative;
			
			if (line == null || line.length() == -1) break;

			line = line.trim();
			if (line.length() == 0) {
			    break;
			}
			      
			Query query = parser.parse(line);
			if(CLASSIC){
				TopDocs results = ssearcher.search(query, MAX_DOCS);
			    ScoreDoc[] hits = results.scoreDocs;
			    
			    int numTotalHits = results.totalHits;
			    for (int i = 0; i < Math.min(MAX_DOCS, numTotalHits); i++) {
			    	Document doc = ssearcher.doc(hits[i].doc);
				    String id = doc.get("id").trim();
				    Float score = new Float(hits[i].score);
				    
				    System.out.println(rcq.id+" Q0 "+id+" "+(i+1)+" "+String.format(Locale.ENGLISH, "%.4f", score.floatValue())+" tfidf");
			      }

		      } else {
		    	  QueryParser sparser = new QueryParser(Version.LUCENE_44, "docs", sanalyzer);
		    	  
		    	  StringBuffer ids = new StringBuffer();
		    	  HashMap<String, Float> sentIdScores= new HashMap<String, Float>();
		    	  
				  //System.err.println("Searching for: "+query.toString());
				  
				  TopDocs results = fsearcher.search(query, K);
				  ScoreDoc[] hits = results.scoreDocs;
				  
				  int n = results.totalHits;
				  
				  for (int i = 0; i < Math.min(K, n); i++) {
			    	Document doc = fsearcher.doc(hits[i].doc);
				    String id = doc.get("id");
				    Float score = new Float(hits[i].score);
				    //System.err.println("found doc: "+id+" : "+score);
				    ids.append(id);
				    ids.append(" ");
				    sentIdScores.put(id, new Float(score));
			      }
				  
				  //now send the docs to the second order index
				  Query squery = sparser.parse(ids.toString().trim());
				  //System.err.println("Second Order search: searching for: "+squery.toString());
				  
				  TopDocs sresults = ssearcher.search(squery, MAX_DOCS);
				  ScoreDoc[] shits = sresults.scoreDocs;
				  
				  Vector<WeightedDocument> wresults = new Vector<WeightedDocument>();
				  
				  int sn = sresults.totalHits;
				  for (int i = 0; i < Math.min(MAX_DOCS, sn); i++) {
				    Document doc = ssearcher.doc(shits[i].doc);
					String sid = doc.get("id"); //the final document to be ranked
					String docScores = doc.get("sentscores").trim();
					//System.err.println(sid);
					if(docScores !=null) {
						WeightedDocument wd = new WeightedDocument(sid, docScores, i);
						wd.setScore(sentIdScores);
						wresults.add(wd);
					} else {
						//System.err.println("no sentence scores(!)");
					}
				  }
					  
				  Collections.sort(wresults);
				  for(int k=0; k< wresults.size(); k++){
					  WeightedDocument wd = wresults.elementAt(k);
					  String id =wd.getID();
					  Float score = wd.getScore();
					  
					  System.out.println(rcq.id+" Q0 "+id+" "+(k+1)+" "+String.format(Locale.ENGLISH, "%.4f", score.floatValue())+" sosim");
				  }
					  
		      }
			      

		    }
		    //searcher.close();
		    freader.close();
		    sreader.close();
		  }
			  
}
