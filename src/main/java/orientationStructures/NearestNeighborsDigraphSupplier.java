/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructures;

import java.util.List;
import java.util.Map;

import org.jgrapht.opt.graph.sparse.SparseIntDirectedWeightedGraph;

import readWrite.StringPairWithWeight;

/**
 * Supplies a directed weighted k-nearest-neighbor (k-NN) graph from weighted
 * edge records.
 * <p>
 * Implementations may support either undirected or directed input edge data,
 * but expose a common pipeline:
 * set records, build vertex indexing, construct a raw sparse graph, restrict to
 * an appropriate core (for example, 2-core), and then derive a k-NN digraph.
 *
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 */
public interface NearestNeighborsDigraphSupplier {
	/**
	 * Sets the input edge records used by the supplier.
	 *
	 * @param edgeRecords list of weighted source-target edge records
	 */
	public void setSourceTargetWeightList(List<StringPairWithWeight> edgeRecords);

	/**
	 * Builds the lookup map from object label to internal integer vertex index.
	 */
	public void buildObjectIndex();// can we implement it here?

	/**
	 * Builds the raw sparse weighted graph from edge records.
	 *
	 * @param edgeRecords edge records used to initialize the raw graph
	 */
	public void buildRawSparseGraph(List<StringPairWithWeight> edgeRecords);

	/**
	 * Determines the induced subgraph used for downstream nearest-neighbor
	 * construction, typically the maximal subgraph in which every vertex has degree
	 * at least 2.
	 */
	public void determineTwoCore();

	/**
	 * Builds the directed weighted k-nearest-neighbor graph from the processed
	 * source graph.
	 *
	 * @param k number of nearest outgoing neighbors retained per vertex
	 */
	public void buildKNearNbrDigraph(int k);

	/**
	 * Returns the object-label to vertex-index lookup.
	 *
	 * @return map from object string identifier to integer vertex index
	 */
	public Map<String, Integer> getVertexIndex();

	/**
	 * Returns the final directed weighted k-nearest-neighbor graph.
	 *
	 * @return k-NN digraph used as input for rank-based linkage
	 */
	public SparseIntDirectedWeightedGraph getKNNDigraph();

}