/* THIS PUBLIC DOMAIN SOFTWARE WAS PRODUCED BY AN EMPLOYEE OF U.S. GOVERNMENT 
 * AS PART OF THEIR OFFICIAL DUTIES.
 */
package algorithmTests;

import java.util.Random;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import algorithms.TwoCore;

/**
 * Test the {@link utilities.TwoCore} class.
 * 
 * @author <a href="https://github.com/probabilist-us">R. W. R. Darling</a>
 *
 */
public class TwoCoreTest {

	SimpleGraph<String, DefaultEdge> graph;
	TwoCore<String, DefaultEdge> tc;

	public TwoCoreTest(SimpleGraph<String, DefaultEdge> myGraph) {
		this.graph = myGraph;
		this.tc = new TwoCore<>(this.graph);
		this.tc.computeTwoCore();

	}

	/**
	 * A random graph one eleven vertices is created. Edge list is inspected. Check
	 * whether two core is computed correctly by {@link utilities.TwoCore} class.
	 * 
	 * @param args none
	 */
	public static void main(String[] args) {
		/*
		 * Random graph on n vertices, m edges
		 */
		SimpleGraph<String, DefaultEdge> graph = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
		String[] vertices = new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k" };
		for (String s : vertices) {
			graph.addVertex(s);
		}
		int n = vertices.length;
		int m = n - 1;
		Random g = new Random();
		int s, t;
		for (int j = 0; j < m; j++) {
			s = g.nextInt(n);
			t = g.nextInt(n - 1);
			if (t >= s) {
				t++;
			}
			graph.addEdge(vertices[s], vertices[t]);
		}
		System.out.println("Edge list of random graph:");
		for (DefaultEdge e : graph.edgeSet()) {
			System.out.print("{" + graph.getEdgeSource(e) + ", " + graph.getEdgeTarget(e) + "}, ");
		}
		System.out.println();
		TwoCoreTest test = new TwoCoreTest(graph);
		test.report();
	}
/**
 * Report results of experiment to console.
 */
	public void report() {
		System.out.println("Two-core vertices:");
		for (String v : this.tc.getTwoCoreVertices()) {
			System.out.print(v + ", ");
		}
		System.out.println();
		System.out.println("Two-core edges:");
		for (DefaultEdge e : this.tc.getTwoCoreEdges()) {
			System.out.print("{" + this.graph.getEdgeSource(e) + ", " + this.graph.getEdgeTarget(e) + "}, ");
		}
		System.out.println();
		System.out.println("Numbers of edges removed per round:");
		for (int i : this.tc.getCountEdgesRemovedEachRound()) {
			System.out.print(i + ", ");
		}
		System.out.println();
	}

}
