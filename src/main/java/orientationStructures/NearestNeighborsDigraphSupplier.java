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
 * weighted graph, and by another class for the directed case.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public interface NearestNeighborsDigraphSupplier {
	public void setSourceTargetWeightList(List<StringPairWithWeight> edgeRecords);

	public void extractTwoCore();

	public void buildObjectIndex();

	public void buildSparseDirectedGraph();

	public Map<String, Integer> getVertexIndex();

	public SparseIntDirectedWeightedGraph getDigraph();

}
