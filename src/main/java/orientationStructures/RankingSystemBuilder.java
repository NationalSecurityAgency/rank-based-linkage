/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructures;

/**
 * Takes a List of {@link readWrite.StringPairWithWeight} records, indexes the
 * source and target objects, and builds two sparse arrays.
 * <p>
 * Needs thought! JGraphT 
 * <p>
 * r[i][j] contains the rank which object i assigns to object j, up to rank K
 * <p>
 * 
 * rankTable[x][y] = k means y is the k-th nearest neighbor of x, according to
 * x's ranking. The same relationship is expressed by orderedNeighbors[x][k] =
 * y, where the objects are treated as the set S:={0, 1, 2, ..., n-1}, and ranks
 * k are elements of {1, 2, ...,n-1}. By convention rankTable[x][x] = 0.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class RankingSystemBuilder {

	/**
	 * 
	 */
	public RankingSystemBuilder() {
		// TODO Auto-generated constructor stub
	}

}
