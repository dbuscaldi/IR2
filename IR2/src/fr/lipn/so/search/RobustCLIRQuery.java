package fr.lipn.so.search;

public class RobustCLIRQuery {
	public String id;
	public String title;
	public String description;
	public String narrative;
	
	public RobustCLIRQuery(String id, String title, String desc,
			String narr) {
		this.id=id;
		this.title=title;
		this.description=desc;
		this.narrative=narr;
	}

}
