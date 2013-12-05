package fr.lipn.so.ranking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public class WeightedDocument implements Comparable<WeightedDocument> {
	protected Float weight;
	private String docID;
	private Vector<HashMap<String,Float>> sscores; //for each sentence, its hashmap docID -> score
	private int index;
	
	public WeightedDocument(String docID, String sentenceScores, int index){
		this.docID=docID;
		this.index=index; //index in hits
		this.sscores= new Vector<HashMap<String,Float>>();
		String [] sentences = sentenceScores.split("\\|");
		for(String s : sentences) {
			HashMap<String, Float> tm = new HashMap<String, Float>();
			s=s.trim();
			String [] items = s.split(" ");
			for(String item : items) {
				if(item.trim().length() > 0) {
					String [] kval = item.split(":");
					tm.put(kval[0], new Float(kval[1]));
				}
			}
		}
		
		this.weight=new Float(0);
	}
	
	public void setScore(HashMap<String, Float> queryScores){
		//set the score of this document according to the query scores
		Float maxScore=this.weight; //should be 0 at start
		for(HashMap<String, Float> map : this.sscores){
			HashSet<String> uniqueIDs = new HashSet<String>();
			uniqueIDs.addAll(queryScores.keySet());
	    	uniqueIDs.retainAll(map.keySet()); //intersection
	    	
	    	if(uniqueIDs.size() > 0) {
	    	
	    		double sum=0d;
		    	for(String key : uniqueIDs) {
		    		
		    		Float v1 = queryScores.get(key);
		    		double s1;
		    		if(v1==null) s1=0d;
		    		else s1=v1.doubleValue();
		    		
		    		Float v2 = map.get(key);
		    		double s2;
		    		if(v2==null) s2=0d;
		    		else s2=v2.doubleValue();
		    		
		    		//System.err.println("common doc: "+key+" s1: "+s1+" s2: "+s2);
		    		
		    		sum+=(Math.sqrt(Math.pow((s1-s2), 2)))/Math.max(s1, s2);
		    	}
		    	
		    	double tmpWeight=1-(sum/(double)uniqueIDs.size());
		    	System.err.println("sentence similarity: "+tmpWeight);
		    	//Document similarity is the max of all sentence similarities
		    	if(tmpWeight > this.weight.doubleValue()) {
		    		this.weight = new Float(tmpWeight);
		    		System.err.println("updating document similarity");
		    	}
	    	}
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

	public Float getScore() {
		return this.weight;
	}

}
