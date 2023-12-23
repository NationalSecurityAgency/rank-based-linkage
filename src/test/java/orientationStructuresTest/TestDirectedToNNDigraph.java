/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructuresTest;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.jgrapht.alg.util.Triple;

import logging.GlobalLog;
import orientationStructures.DirectedToNNDigraph;
import readWrite.StringPairCSVReader;

/**
 * Tests the {@link orientationStructures.DirectedToNNDigraph} class.
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class TestDirectedToNNDigraph {
	DirectedToNNDigraph d2nndg;
	StringPairCSVReader reader;

	/**
	 * 
	 */
	public TestDirectedToNNDigraph() {
		GlobalLog.log(Level.INFO, "Instance of " + TestDirectedToNNDigraph.class.getName());
		this.reader = new StringPairCSVReader();
		this.d2nndg = new DirectedToNNDigraph(true); // higher weight is better
	}

	/**
	 * @param args fileName, # neighbors
	 */
	public static void main(String[] args) {
		String fileName = args[0];
		int k = Integer.parseInt(args[1]); // # neighbors
		GlobalLog.log(Level.INFO,"Input file: " + fileName);
		TestDirectedToNNDigraph test = new TestDirectedToNNDigraph();
		test.reader.readStringPairsWithWeights(fileName);
		int nEdges = test.reader.getWeightedEdges().size();
		GlobalLog.log(Level.INFO, "Edge list has size " + nEdges);
		/*
		 * (1) Build raw sparse directed weigted linkageGraph
		 */
		double start = (double) System.currentTimeMillis();
		GlobalLog.log(Level.INFO, "Edge list has been passed to DirectedToNNDigraph.");
		test.d2nndg.buildRawSparseGraph(test.reader.getWeightedEdges());
		double duration = (double) (System.currentTimeMillis() - start) / 1000.0;
		GlobalLog.log(Level.INFO, "Sparse directed linkageGraph built in " + duration + " seconds.");
		/*
		 * Inspect vertex index map
		 */
		Random g = new Random();
		int index = g.nextInt(test.d2nndg.getnV());
		String image = test.d2nndg.getObjectStringLookup().get(index);
		GlobalLog.log(Level.INFO, "Random source vertex: index = " + index + "; string = " + image);
		GlobalLog.log(Level.INFO, "Reverse lookup for image: " + test.d2nndg.getVertexIndex().get(image));
		/**
		 * (2) Determine two core. Needed to exclude non-two core vertices and edges
		 * from K-NN digraph
		 */
		test.d2nndg.determineTwoCore();
		/*
		 * Inspect kNearNbrOfVertex() method, which will be used for K-NN digraph.
		 */
		List<Triple<Integer, Integer, Double>> nbrTest = test.d2nndg.kNearNbrOfVertex(index, k).toList();
		int index0 = nbrTest.get(0).getSecond();
		String image0 = test.d2nndg.getObjectStringLookup().get(index0);
		double weight = nbrTest.get(0).getThird();
		GlobalLog.log(Level.INFO, "Source: " + image + " Target: " + image0 + " Weight: " + weight);
		/*
		 * (3) Build K-NN digraph
		 */
		start = (double) System.currentTimeMillis();
		test.d2nndg.buildKNearNbrDigraph(k);
		duration = (double) (System.currentTimeMillis() - start) / 1000.0;
		GlobalLog.log(Level.INFO, " For K = " + k + ", K-NN digraph built in " + duration + " seconds.");
		int nEKNN = test.d2nndg.getKNNDigraph().edgeSet().size();
		GlobalLog.log(Level.INFO, "K-NN digraph has " + nEKNN + " edges.");

	}

}
