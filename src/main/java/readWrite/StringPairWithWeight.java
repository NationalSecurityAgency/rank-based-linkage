/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package readWrite;

import java.util.List;

/**
 * Record class to represent a weighted directed edge.
 * <p>
 * Even when similarity between x and y is symmetric, the relation "is one of
 * the k nearest neighbors of" is not symmetric.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public record StringPairWithWeight(String source, String target, double weight) {

// superfluous, since default for record does the same thing
	/*
	 * public boolean equals(Object obj) { return
	 * this.toString().equals(obj.toString()); }
	 */

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public String toString() {
		return source + " " + target + " " + Double.toString(weight);
	}
/**
 * 
 * @return list of two strings
 */
	public List<String> getSourceAndTarget() {
		return List.of(source, target);
	}

}
