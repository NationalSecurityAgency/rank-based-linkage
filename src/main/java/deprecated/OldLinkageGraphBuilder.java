/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package deprecated;

import java.util.ArrayList;
import java.util.List;

import orientationStructures.NeighborRecord;

/**
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class OldLinkageGraphBuilder {
	final int K;
	List<NeighborRecord> partialRankings;
	

	/**
	 * 
	 */
	public OldLinkageGraphBuilder(int numNeighbors) {
		this.K = numNeighbors;
	}

	/**
	 * @return the partialRankings
	 */
	public List<NeighborRecord> getPartialRankings() {
		return partialRankings;
	}

	/**
	 * @param partialRankings the partialRankings to set
	 */
	public void setPartialRankings(List<NeighborRecord> partialRankings) {
		this.partialRankings = partialRankings;
	}

	/**
	 * 
	 * @param rankTable an n &times; n array. Whenever object j is at rank r
	 *                  according to i, add triple (i, j, r) to the partial rankings
	 *                  provided 0 &lt; r &le; K.
	 */
	public void setPartialRankings(int[][] rankTable) {
		this.partialRankings = new ArrayList<>();
		int r;
		for (int i = 0; i < rankTable.length; i++) {
			for (int j = 0; j < rankTable.length; j++) {
				r = rankTable[i][j];
				if (r > 0 && r <= this.K) {
					this.partialRankings.add(new NeighborRecord(Integer.toString(i), Integer.toString(j), r));
				}

			}
		}
	}

}
