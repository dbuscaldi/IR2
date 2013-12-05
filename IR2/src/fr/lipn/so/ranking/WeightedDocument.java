package fr.lipn.so.ranking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public class WeightedDocument implements Comparable<WeightedDocument> {
	protected Double weight;
	private String docID;
	private Vector<HashMap<String,Double>> sscores; //for each sentence, its hashmap docID -> score
	private int index;
	private Double baseScore;
	
	public WeightedDocument(String docID, String sentenceScores, int index, double bm25score){
		this.docID=docID;
		this.index=index; //index in hits
		this.sscores= new Vector<HashMap<String,Double>>();
		String [] sentences = sentenceScores.split("\\|");
		for(String s : sentences) {
			HashMap<String, Double> tm = new HashMap<String, Double>();
			s=s.trim();
			String [] items = s.split(" ");
			for(String item : items) {
				if(item.trim().length() > 0) {
					String [] kval = item.split(":");
					tm.put(kval[0], new Double(kval[1]));
				}
			}
			sscores.add(tm);
		}
		
		this.weight=new Double(0);
		this.baseScore=bm25score;
	}
	
	public void setScore(HashMap<String, Double> queryScores){
		//set the score of this document according to the query scores
		
		//System.err.println("setting scores for document "+this.docID);
		
		double sumScores=0d;
		int numnotZero=0;
		
		for(HashMap<String, Double> map : this.sscores){
			HashSet<String> uniqueIDs = new HashSet<String>();
			uniqueIDs.addAll(queryScores.keySet());
	    	uniqueIDs.retainAll(map.keySet()); //intersection
	    	
	    	if(uniqueIDs.size() > 0) {
	    		double saliency=(double)uniqueIDs.size()/(double)map.keySet().size();
	    		//System.err.println("saliency factor: "+saliency);
	    		
	    		double sum=0d;
		    	for(String key : uniqueIDs) {
		    		
		    		Double v1 = queryScores.get(key);
		    		double s1;
		    		if(v1==null) s1=0d;
		    		else s1=v1.doubleValue();
		    		
		    		Double v2 = map.get(key);
		    		double s2;
		    		if(v2==null) s2=0d;
		    		else s2=v2.doubleValue();
		    		
		    		sum+=(Math.sqrt(Math.pow((s1-s2), 2)))/Math.max(s1, s2);
		    	}
		    	
		    	double tmpWeight=(1-(sum/(double)uniqueIDs.size()))*saliency; //includes saliency or normalization factor
		    	//double tmpWeight=1-(sum/(double)uniqueIDs.size()); //old formula
		    	//System.err.println("sentence similarity: "+tmpWeight);
		    	sumScores+=tmpWeight;
		    	numnotZero++;
		    	
		    	//Uncomment this if document similarity is the max of all sentence similarities:
		    	/*
		    	if(tmpWeight > this.weight.doubleValue()) {
		    		this.weight = new Double(tmpWeight);
		    	
		    	}
		    	*/
	    	}
		}
		
		//Uncomment this if document similarity is ANZ over sentence similarities:
		if(sumScores > 0) {
			this.weight=sumScores/(double)numnotZero;
		}
		
	}
	
	@Override
	public int compareTo(WeightedDocument o) {
		return (-this.weight.compareTo(o.weight));
	}
	
	public int getIndex(){
		return this.index;
	}

	public String getID() {
		return this.docID;
	}

	public Double getScore() {
		return this.weight;
	}

	public double baseScore() {
		return this.baseScore.doubleValue();
	}

}
