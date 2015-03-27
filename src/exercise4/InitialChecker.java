package exercise4;

import peersim.config.Configuration;
import exercise2.InitializerEx2;

public class InitialChecker extends InitializerEx2 {
	
	final int maxsize;
	
	public InitialChecker(final String prefix_)
	{
		super(prefix_);
		maxsize = Configuration.getInt("maxisze");
		
	}

}
