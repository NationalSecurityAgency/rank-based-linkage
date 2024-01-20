# rank-based-linkage

Rank-based linkage is a scalable algorithm for graph summarization and clustering. 
Input is an edge-weighted (di)graph. 
Instructions to execute on the java command line are found in the javadoc for the ClusteringByInsway class.
Mathematical foundations are explained in the paper:
<i>Rank-based linkage I: triplet comparisons and oriented simplicial complexes</i>, 
Darling, Grilliette and Logan, https://arxiv.org/abs/2302.02200

## Usage
<p>Format of the CSV input file: each line is: source, target, weight.
<p>Lines refer to edges of a graph, which may be directed or undirected.
<p>Examples: 
<p>"java -jar rbl.jar edgefileD.csv Dh 8 50"; graph is directed, higher weight means more similar
<p>"java -jar rbl.jar edgefileD.csv Dl 8 50"; graph is directed, lower weight means more similar
<p>"java -jar rbl.jar edgefileU.csv Uh 16"; graph is undirected, higher weight means more similar
<p>"java -jar rbl.jar edgefileU.csv Ul 16"; graph is undirected, lower weight means more similar.
<p>First integer (e.g. 8) is number of near neighbors to be ranked.
<p>Optional second integer (e.g. 50) is maximum cluster size to be used in setting in-sway threshold.

<p>Full details: see Javadoc for the ClusteringByInsway class.

<p> The output is a CSV file containing the linkage graph, or a prominent part of it. Each row is a pair of objects ("mutual friends" in the language of the paper) and an integer representing the in-sway of these two objects. There is also an HTML log file describing what happened in the rbl computation.

<p> In the resources folder are two .csv files containing directed weighted graph data used in our paper. Here "Dh" is the appropriate setting.

## Future plans
<p> (1) Create a Java preprocessor for network traffic data where each record includes a record ID, a source address, a destination address, and a time stamp. Build an undirected line graph, where records e and f are adjacent if the destination of e is the source of f, and e precedes f in time. Record e orders its neighbors by absolute difference between their time stamps and e's own time stamp. The preprocessor exports a CSV file whose lines are of the form: 
e (record ID), f (record ID), time difference between e and f. 
This file becomes an input for rbl.jar with Ul as input parameter.

<p> (2) Create a Java preprocessor for bipartite transactional data where each each record includes a consumer ID, a product ID, and a numerical quantity. Suppose the goal is unsupervised learning for products. Construct the transition matrix for the Markov chain whose state space is product IDs, and whose transition rate p(x, y) from product x to product y is computed as follows. Sample at random a consumer who bought product x, weighted by quantity. Then sample at random a product purchased by this consumer, again weighted by quantity, and denote this random product ID by Z. Take p(x,y) to be the probability of {Z=y}. The preprocessor exports a CSV file whose lines are of the form: x (product ID), y (product ID), p(x,y). This file becomes an input for rbl.jar with Dh as input parameter.

<p> (3) Create (in any language) a post-processor which displays a force-directed embedding of a linkage graph, read from the output of rbl.jar. Suitable display tools include NetworkX, iGraph, Mathematica, and NetworkIT.

