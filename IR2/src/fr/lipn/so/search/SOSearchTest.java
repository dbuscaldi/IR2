package fr.lipn.so.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import fr.lipn.so.indexing.BooleanSimilarity;
import fr.lipn.so.ranking.WeightedDocument;

public class SOSearchTest {
	static boolean CLASSIC=false;
	private static int K=70;
	private static int MAX_DOCS=1000;
	
	public static void main(String[] args) throws IOException, ParseException {
		String fIndex = "/tempo/corpora/AQUAINT_indexed";
		String sIndex = "/tempo/corpora/secondIndex";
		
		String field = "text";
		
		String queries = null;
		int repeat = 0;
		boolean raw = false;
		String queryString = null;
		int hitsPerPage = 10;
		
		//FO reader
		IndexReader freader = IndexReader.open(FSDirectory.open(new File(fIndex)));
		IndexSearcher fsearcher = new IndexSearcher(freader);
		Analyzer fanalyzer = new EnglishAnalyzer(Version.LUCENE_44);
		IndexWriterConfig fiwc = new IndexWriterConfig(Version.LUCENE_44, fanalyzer);
		fiwc.setSimilarity(new BM25Similarity());

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

		
		BufferedReader in = null;
		if (queries != null) {
		     in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
		} else {
		     in = new BufferedReader(new InputStreamReader(System.in, "cp1252"));
		}
		QueryParser parser = new QueryParser(Version.LUCENE_44, field, fanalyzer);
		
		
		while (true) {
			if (queries == null && queryString == null) System.out.println("Enter query: ");
			String line = queryString != null ? queryString : in.readLine();
			if (line == null || line.length() == -1) break;

			      line = line.trim();
			      if (line.length() == 0) {
			        break;
			      }
			      
			      Query query = parser.parse(line);
			      if(CLASSIC){
			    	  System.out.println("Searching for: " + query.toString(field));
			            
				      if (repeat > 0) {                           // repeat & time as benchmark
				        Date start = new Date();
				        for (int i = 0; i < repeat; i++) {
				          ssearcher.search(query, null, 100);
				        }
				        Date end = new Date();
				        System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
				      }
				      
				      doPagingSearch(in, ssearcher, query, hitsPerPage, raw, queries == null && queryString == null);
				      
			      } else {
			    	  QueryParser sparser = new QueryParser(Version.LUCENE_44, "docs", sanalyzer);
			    	  
			    	  StringBuffer ids = new StringBuffer();
			    	  HashMap<String, Float> sentIdScores= new HashMap<String, Float>();
			    	  
					  System.err.println("Searching for: "+query.toString());
					  
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
					  System.err.println("Second Order search: searching for: "+squery.toString());
					  
					  TopDocs sresults = ssearcher.search(squery, MAX_DOCS);
					  ScoreDoc[] shits = sresults.scoreDocs;
					  
					  Vector<WeightedDocument> wresults = new Vector<WeightedDocument>();
					  
					  int sn = sresults.totalHits;
					  for (int i = 0; i < Math.min(MAX_DOCS, sn); i++) {
					    Document doc = ssearcher.doc(shits[i].doc);
						String sid = doc.get("id"); //the final document to be ranked
						String docScores = doc.get("sentscores").trim();
						//System.err.println(id);
						if(docScores !=null) {
							WeightedDocument wd = new WeightedDocument(sid, docScores, i);
							wd.setScore(sentIdScores);
							wresults.add(wd);
						} else {
							System.err.println("no sentence scores(!)");
						}
					  }
					  
					  Collections.sort(wresults);
					  
					  doPagingSOS(in, ssearcher, wresults, shits);
					  
			      }
			      

			      if (queryString != null) {
			        break;
			      }
			    }
			    //searcher.close();
			    freader.close();
			    sreader.close();
			  }

			  /**
			   * This demonstrates a typical paging search scenario, where the search engine presents 
			   * pages of size n to the user. The user can then go to the next page if interested in
			   * the next hits.
			   * 
			   * When the query is executed for the first time, then only enough results are collected
			   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
			   * is executed another time and all hits are collected.
			   * 
			   */
			  public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query, 
			                                     int hitsPerPage, boolean raw, boolean interactive) throws IOException {
			 
			    // Collect enough docs to show 5 pages
			    TopDocs results = searcher.search(query, 5 * hitsPerPage);
			    ScoreDoc[] hits = results.scoreDocs;
			    
			    int numTotalHits = results.totalHits;
			    System.out.println(numTotalHits + " total matching documents");

			    int start = 0;
			    int end = Math.min(numTotalHits, hitsPerPage);
			        
			    while (true) {
			      if (end > hits.length) {
			        System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
			        System.out.println("Collect more (y/n) ?");
			        String line = in.readLine();
			        if (line.length() == 0 || line.charAt(0) == 'n') {
			          break;
			        }

			        hits = searcher.search(query, numTotalHits).scoreDocs;
			      }
			      
			      end = Math.min(hits.length, start + hitsPerPage);
			      
			      for (int i = start; i < end; i++) {
			        if (raw) {                              // output raw format
			          System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
			          continue;
			        }

			        Document doc = searcher.doc(hits[i].doc);
			        String path = doc.get("id");
			        if (path != null) {
			          System.out.println((i+1) + ". " + path + " score="+hits[i].score);
			          System.out.println("\tID: " + doc.get("id"));
			          System.out.println("Titre: " + doc.get("title"));
			          String text=doc.get("text");
			          if(text!= null) System.out.println(formatTextWidth(doc.get("text"), 120));
			        } else {
			          System.out.println((i+1) + ". " + "No title for this document");
			        }
			                  
			      }

			      if (!interactive || end == 0) {
			        break;
			      }

			      if (numTotalHits >= end) {
			        boolean quit = false;
			        while (true) {
			          System.out.print("Press ");
			          if (start - hitsPerPage >= 0) {
			            System.out.print("(p)revious page, ");  
			          }
			          if (start + hitsPerPage < numTotalHits) {
			            System.out.print("(n)ext page, ");
			          }
			          System.out.println("(q)uit or enter number to jump to a page.");
			          
			          String line = in.readLine();
			          if (line.length() == 0 || line.charAt(0)=='q') {
			            quit = true;
			            break;
			          }
			          if (line.charAt(0) == 'p') {
			            start = Math.max(0, start - hitsPerPage);
			            break;
			          } else if (line.charAt(0) == 'n') {
			            if (start + hitsPerPage < numTotalHits) {
			              start+=hitsPerPage;
			            }
			            break;
			          } else {
			            int page = Integer.parseInt(line);
			            if ((page - 1) * hitsPerPage < numTotalHits) {
			              start = (page - 1) * hitsPerPage;
			              break;
			            } else {
			              System.out.println("No such page");
			            }
			          }
			        }
			        if (quit) break;
			        end = Math.min(numTotalHits, start + hitsPerPage);
			      }
			    }
			  }
			  
			  
			  public static String formatTextWidth(String input, int maxLineLength) {
				    String [] fragments = input.split("[\\s]+");
				    StringBuilder output = new StringBuilder(input.length());
				    int lineLen = 0;
				    for(String word: fragments) {
				       
				        if (lineLen + word.length() > maxLineLength) {
				            output.append("\n");
				            lineLen = 0;
				        }
				        output.append(word);
				        output.append(" ");
				        lineLen += (word.length()+1);
				    }
				    return output.toString();
			  }
			  
			  public static void doPagingSOS(BufferedReader in, IndexSearcher searcher, Vector<WeightedDocument> rankedDocs, ScoreDoc [] hits) throws IOException {
				  int hitsPerPage= 10 ;
				  
				  int numTotalHits = rankedDocs.size();
				  System.out.println(numTotalHits + " total matching documents");

				  int start = 0;
				  int end = Math.min(numTotalHits, hitsPerPage);
				  while (true) {
					  end = Math.min(rankedDocs.size(), start + hitsPerPage);

					  for (int i = start; i < end; i++) {
						  Document doc = searcher.doc(hits[rankedDocs.elementAt(i).getIndex()].doc);
						  String path = doc.get("id");
						  if (path != null) {
							  System.out.println((i+1) + ". " + path + " score="+hits[i].score);
							  System.out.println("\tID: " + doc.get("id"));
							  System.out.println("Titre: " + doc.get("title"));
							  String text=doc.get("text");
							  if(text!= null) System.out.println(formatTextWidth(doc.get("text"), 120));
						  } else {
							  System.out.println((i+1) + ". " + "No title for this document");
						  }
						  
					  }

					  if (numTotalHits >= end) {
						  boolean quit = false;
						  while (true) {
							  System.out.print("Press ");
							  if (start - hitsPerPage >= 0) {
								  System.out.print("(p)revious page, ");  
							  }
							  if (start + hitsPerPage < numTotalHits) {
								  System.out.print("(n)ext page, ");
							  }
							  System.out.println("(q)uit or enter number to jump to a page.");
							  
							  String line = in.readLine();
							  if (line.length() == 0 || line.charAt(0)=='q') {
								  quit = true;
								  break;
							  }
							  if (line.charAt(0) == 'p') {
								  start = Math.max(0, start - hitsPerPage);
								  break;
							  } else if (line.charAt(0) == 'n') {
								  if (start + hitsPerPage < numTotalHits) {
									  start+=hitsPerPage;
								  }
								  break;
							  } else {
								  int page = Integer.parseInt(line);
								  if ((page - 1) * hitsPerPage < numTotalHits) {
									  start = (page - 1) * hitsPerPage;
									  break;
								  } else {
									  System.out.println("No such page");
								  }
							  }
						  }
						  if (quit) break;
						  end = Math.min(numTotalHits, start + hitsPerPage);
					  }
				  }
			  }
}

