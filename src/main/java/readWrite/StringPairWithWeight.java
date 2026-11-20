/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package readWrite;

import java.util.List;

/**
 * Immutable weighted directed edge between two string-identified vertices.
 * <p>
 * Even when a similarity function between {@code x} and {@code y} is symmetric,
 * nearest-neighbor relationships are generally directed: {@code x} being among the
 * {@code k} nearest neighbors of {@code y} does not imply the converse.
 * <p>
 * This record stores the ordered pair ({@code source}, {@code target}) together
 * with a numeric {@code weight} associated with the directed edge.
 *
 * @param source the origin vertex identifier
 * @param target the destination vertex identifier
 * @param weight the edge weight (for example, a distance, dissimilarity, or score)
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 */
public record StringPairWithWeight(String source, String target, double weight) {


	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public String toString() {
		return source + " " + target + " " + Double.toString(weight);
	}
/**
 * Returns the ordered endpoint labels of this directed edge.
 * <p>
 * The returned list has length two and preserves edge direction:
 * index 0 is {@code source}, and index 1 is {@code target}.
 *
 * @return immutable two-element list {@code [source, target]}
 */
	public List<String> getSourceAndTarget() {
		return List.of(source, target);
	}

}