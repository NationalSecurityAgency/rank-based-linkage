/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructures;

import java.util.List;
import java.util.Map;

import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;

import readWrite.StringPairWithWeight;

/**
 * This interface is implemented by a class which reads edges of an undirected
 * weighted linkageGraph, and by another class for the directed case.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public interface NearestNeighborsDigraphSupplier {
	/**
	 * 
	 * @param edgeRecords List of records to become weighted edges
	 */
	public void setSourceTargetWeightList(List<StringPairWithWeight> edgeRecords);

	/**
	 * Build lookup from object string to int
	 */
	public void buildObjectIndex();// can we implement it here?

	/**
	 * 
	 * @param edgeRecords to be used for raw graph
	 */
	public void buildRawSparseGraph(List<StringPairWithWeight> edgeRecords);

	/**
	 * Find maximal subgraph where every vertex has at least 2
	 */
	public void determineTwoCore();

	/**
	 * 
	 * @param k number of neighbors
	 */
	public void buildKNearNbrDigraph(int k);

	/**
	 * 
	 * @return lookup from object string to int
	 */
	public Map<String, Integer> getVertexIndex();

	/**
	 * 
	 * @return the NN digraph, used as input for rank-based linkage
	 */
	public SparseIntDirectedWeightedGraph getKNNDigraph();

}
