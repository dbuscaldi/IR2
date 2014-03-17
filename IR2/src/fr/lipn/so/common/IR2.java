package fr.lipn.so.common;

public class IR2 {
	
	//group 1
	public final static int IRSIM_ONLY=0; //use only IRsim
	public final static int DOCVECS=1; //use only docvector score (the score is bm25 calculated on document vectors)
	public final static int IRSIM_DOCVECS_COMB=2; //Geometric mean between document docvector scores and IRsim
	public final static int HYBRID_TEXT_SEM=3; //Geometric mean between the textual score and the IRsim score
	
	//group 2
	public final static int ANZ=0;
	public final static int MAX=1;
	
	
	//public static int DOCWEIGHT=IRSIM_ONLY; //one from group 1
	public static int DOCWEIGHT=IRSIM_DOCVECS_COMB; //one from group 1
	public static int PHRASE_COMB=ANZ; //one from group 2
	
	public static boolean TOP_PRIORITY=true; //give more weight to the first sentences
	
	public static String RELEVANT_FIELD="text"; //this indicates the relevant field to be used by the secondIndexer
	
	//public static int SENTENCE_LIMIT=3; //this indicates the maximum number of sentences to be processed for each document
	
}
