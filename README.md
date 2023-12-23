# rank-based-linkage

Rank-based linkage is a scalable algorithm for graph summarization and clustering. 
Input is an edge-weighted (di)graph. 
Instructions to execute on the java command line are found in the javadoc for the ClusteringByInsway class.
Mathematical foundations are explained in the paper:
<i>Rank-based linkage I: triplet comparisons and oriented simplicial complexes</i>, 
Darling, Grilliette and Logan, https://arxiv.org/abs/2302.02200

## Usage
<p>Format of the csv input file: each line is: source, target, weight.
<p>Lines refer to edges of a graph, which may be directed or undirected.
<p>Examples: 
<p>"java -jar rbl.jar edgefileD.csv Dh 8 50"; graph is directed, higher weight means more similar
<p>"java -jar rbl.jar edgefileU.csv Ul 16"; graph is directed, lower weight means more similar
<p>"java -jar rbl.jar edgefileD.csv Dh 8 50"; graph is undirected, higher weight means more similar
<p>"java -jar rbl.jar edgefileU.csv Ul 16"; graph is undirected, lower weight means more similar.
<p>First integer (e.g. 8) is number of near neighbors to be ranked.
<p>Optional second integer (e.g. 50) is maximum cluster size to be used in setting in-sway threshold.

<p>Full details: see Javadoc for the ClusteringByInsway class.

