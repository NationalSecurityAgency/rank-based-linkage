/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithmTests;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.alg.util.UnionFind;

/**
 * 
 * Demonstrates how to report the largest cluster size during union find.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 */
public class UnionFindTest {
	UnionFind<String> uf;
	Set<String> objects;

	/**
	 * Toy example with eight elements
	 */
	public UnionFindTest() {
		this.objects = Set.of("a", "b", "c", "d", "e", "f", "g", "h");
		this.uf = new UnionFind<>(objects);
	}

	/**
	 * @param args none
	 */
	public static void main(String[] args) {
		UnionFindTest test = new UnionFindTest();
		test.uf.union("a", "c");
		test.uf.union("f", "h");
		System.out.println("Parent of a: " + test.uf.find("a") + ". Parent of c: " + test.uf.find("c"));
		test.uf.union("c", "h");
		System.out.println("Parent of f: " + test.uf.find("f"));
		test.uf.union("b", "e");
		/*
		 * Essential part
		 */
		Map<String, List<String>> clusters = test.objects.stream().collect(Collectors.groupingBy(s -> test.uf.find(s)));
		int maxClusterSize = clusters.values().stream().mapToInt(c -> c.size()).max().orElse(0);
		System.out.println("max cluster size: " + maxClusterSize);
		/*
		 * For diagnosis, not essential
		 */
		System.out.println("clusters value for key a: " + clusters.get("a").toString());
		int[] clusterSizes = clusters.values().stream().mapToInt(c -> c.size()).toArray();
		System.out.println("cluster sizes: " + Arrays.toString(clusterSizes));
		/*
		 * Avoid intermediate map? Yes.
		 */
		int maxSize2 = test.objects.stream().collect(Collectors.groupingBy(s -> test.uf.find(s)))
				.values().stream().mapToInt(c -> c.size()).max().orElse(0);
		System.out.println("max cluster size: " + maxSize2);
	}

}
