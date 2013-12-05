package fr.lipn.so.common;

public class IR2 {
	
	//group 1
	public final static int IRSIM_ONLY=0; //use only IRsim
	public final static int DOCVECS=1; //use only docvector score
	public final static int IRSIM_DOCVECS_COMB=2; //Geometric mean between document docvector scores and IRsim
	
	//group 2
	public final static int ANZ=0;
	public final static int MAX=1;
	
	
	public static int DOCWEIGHT=DOCVECS; //one from group 1
	public static int PHRASE_COMB=ANZ; //one from group 2
	
	
}
