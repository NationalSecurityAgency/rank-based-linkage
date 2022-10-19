/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package readWrite;

/**
 * Ingested data consists of many lines, where each line contains a triple
 * interpreted as a record of this type.
 * <p>
 * First interpretation: weighted undirected edge
 * <p>
 * Second interpretation: weighted directed edge. In that case there might be
 * another record where the source and target strings are switched and the
 * weight may be different.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public record StringPairWithWeight(String source, String target, double weight) {

	@Override
	public boolean equals(Object obj) {
		return this.toString().equals(obj.toString());
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public String toString() {
		return source + " " + target + " " + Double.toString(weight);
	}

}
