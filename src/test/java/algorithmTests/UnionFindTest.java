/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithmTests;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jgrapht.alg.util.UnionFind;

/**
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class UnionFindTest {

	/**
	 * 
	 */
	public UnionFindTest(int n) {
		Set<Integer> objects = IntStream.range(0, n).boxed().collect(Collectors.toSet());
		UnionFind<Integer> uf = new UnionFind<>(objects);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
