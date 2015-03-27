package exercise1;

import java.util.Random;

import peersim.config.Configuration;
import peersim.dynamics.WireGraph;
import peersim.graph.Graph;

public class WireRandom extends WireGraph
{
	private static final String NETWORK_SIZE = "network.size";
	private static final String DEGREE_MIN = "degreeMin";
	private static final String DEGREE_MAX = "degreeMax";

	private final int _networkSize;
	private final int _degreeMin;
	private final int _degreeMax;
	private final String edges;

	public WireRandom(final String prefix_)
	{
		super(prefix_);
		_networkSize = Configuration.getInt(NETWORK_SIZE);
		_degreeMin = Configuration.getInt(prefix_ + "." + DEGREE_MIN, 1);
		_degreeMax = Configuration.getInt(prefix_ + "." + DEGREE_MAX, 100);
		edges = Configuration.getString(prefix_+".edges");
	}

	@Override
	public void wire(final Graph g)
	{
		final Random randomDegree = new Random(1);
		final Random randomEdge = new Random(10);
		if(edges.length()==0)
		{
			for(int i = 0; i < g.size() ; i++)
			{
				final int degree = randomDegree.nextInt((_degreeMax - _degreeMin)) + _degreeMin;

				for(int j = 0; j < degree ; j++)
				{
					final int other = randomEdge.nextInt(_networkSize);

					if(i != other)
					{
						g.setEdge(i, other);
						g.setEdge(other, i);
					}
				}
			}
		}
		else
		{
			StringBuilder s = new StringBuilder();
			for(int i=0;i<edges.length();i++)
			{
				int start,end;
				if(edges.charAt(i)=='(')
				{	
					int j,k;
					for(j=i+1;j<edges.length()&&edges.charAt(j)!=',';j++)
						s.append(edges.charAt(j));
					start=Integer.parseInt(s.toString());
					System.out.print("("+start+",");
					s=new StringBuilder();
					for(k=j+1;k<edges.length()&&edges.charAt(k)!=')';k++)
						s.append(edges.charAt(k));
					end=Integer.parseInt(s.toString());
					System.out.println(end+")");
					s=new StringBuilder();
					g.setEdge(start, end);
					g.setEdge(end, start);
					i=k;
				}
			}
		}
	}

}
