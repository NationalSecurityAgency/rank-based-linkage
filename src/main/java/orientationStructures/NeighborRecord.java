/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructures;

/**
 * When x is source, y is target, the record indicates that y is the k-th
 * nearest neighbor of x, according to x's ranking. Such records are used in
 * {@link algorithms.LinkageGraphBuilder}
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public record NeighborRecord(String source, String target, int rank) {
	/**
	 * Concatenate three string fields, with blank space separators.
	 */
	@Override
	public String toString() {
		return source + " " + target + " " + Integer.toString(rank);
	}
	// superfluous, since default for record does the same thing
	/*
	 * public boolean equals(Object obj) { return
	 * this.toString().equals(obj.toString()); }
	 */

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

}
