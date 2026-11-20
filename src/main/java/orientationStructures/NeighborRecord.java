/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package orientationStructures;

/**
 * Immutable record describing a directed nearest-neighbor relation.
 * <p>
 * When {@code source = x} and {@code target = y}, this record states that
 * {@code y} is the {@code rank}-th nearest neighbor of {@code x} according to
 * {@code x}'s ranking.
 * <p>
 * Such records are used by {@link algorithms.LinkageGraphBuilder} to build
 * orientation/linkage structures from ranked neighbor data.
 *
 * @param source the vertex whose neighborhood ranking is being referenced
 * @param target the ranked neighbor vertex
 * @param rank the 1-based position of {@code target} in {@code source}'s
 *             nearest-neighbor order
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 */
public record NeighborRecord(String source, String target, int rank) {
	/**
	 * Returns a compact space-delimited representation:
	 * {@code "source target rank"}.
	 *
	 * @return string form of this record suitable for plain-text export
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