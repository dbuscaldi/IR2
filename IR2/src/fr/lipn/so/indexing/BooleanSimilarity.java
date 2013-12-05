package fr.lipn.so.indexing;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;


public class BooleanSimilarity extends SimilarityBase {

	@Override
	protected float score(BasicStats arg0, float arg1, float arg2) {
		return 1f; //if the term is in the document, its score is 1
	}

	@Override
	public String toString() {
		return "Boolean Similarity, no parameters needed";
	}

}
