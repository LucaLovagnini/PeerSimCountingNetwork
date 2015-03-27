package cycleDrivenFlooding;

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

	public WireRandom(final String prefix_)
	{
		super(prefix_);
		_networkSize = Configuration.getInt(NETWORK_SIZE);
		_degreeMin = Configuration.getInt(prefix_ + "." + DEGREE_MIN, 1);
		_degreeMax = Configuration.getInt(prefix_ + "." + DEGREE_MAX, 100);
	}

	@Override
	public void wire(final Graph g)
	{
		final Random randomDegree = new Random(1);
		final Random randomEdge = new Random(10);

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

}
