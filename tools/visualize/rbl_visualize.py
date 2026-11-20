"""Force-directed display of a linkage graph written by rbl.jar.

rbl.jar writes a folder named RBL-...-NN-<timestamp> containing

    <L>-modest-edges.csv                   header: component,source-vertex,target-vertex,weight
    <s>-singletons-among-<n>-objects.csv   header: isolated vertex

Each edge row is a pair of mutual friends with its in-sway as weight, and the
component number of the pair in the linkage subgraph. This script reads those
files, lays out the graph with a seeded spring layout, colours objects by
component, scales edge width by in-sway, and writes a PNG/SVG/PDF (via
matplotlib) and/or a self-contained interactive HTML page (inline SVG, no
external scripts).
"""

import argparse
import csv
import glob
import html
import math
import os
import sys

import networkx as nx

EDGE_HEADER = ["component", "source-vertex", "target-vertex", "weight"]


def find_output_files(path):
    """Return (edge_csv, singleton_csv or None) for an rbl.jar output folder or edge file."""
    if os.path.isdir(path):
        edges = glob.glob(os.path.join(path, "*-modest-edges.csv"))
        if len(edges) != 1:
            raise FileNotFoundError("expected one *-modest-edges.csv in " + path + ", found " + str(len(edges)))
        singles = glob.glob(os.path.join(path, "*-singletons-among-*-objects.csv"))
        return edges[0], (singles[0] if len(singles) == 1 else None)
    return path, None


def read_linkage_graph(edge_csv, singleton_csv=None):
    """Build an undirected networkx graph from rbl.jar output.

    Node attribute "component" is the linkage-subgraph component number
    (-1 for isolated objects). Edge attribute "insway" is the in-sway.
    """
    g = nx.Graph()
    with open(edge_csv, newline="") as f:
        rows = csv.reader(f)
        header = next(rows)
        if [h.strip() for h in header] != EDGE_HEADER:
            raise ValueError("unexpected header in " + edge_csv + ": " + ",".join(header))
        for row in rows:
            if not row:
                continue
            c, s, t, w = int(row[0]), row[1], row[2], int(row[3])
            g.add_edge(s, t, insway=w)
            g.nodes[s]["component"] = c
            g.nodes[t]["component"] = c
    if singleton_csv is not None:
        with open(singleton_csv, newline="") as f:
            rows = csv.reader(f)
            next(rows)
            for row in rows:
                if row and row[0] not in g:
                    g.add_node(row[0], component=-1)
    return g


def layout(g, seed=1, iterations=200):
    """Seeded spring layout of each connected piece, pieces packed in rows, largest first.

    Inside a piece, higher in-sway pulls objects closer (spring weight rescaled to 0.2..1).
    Isolated objects are placed in a grid after the pieces.
    """
    if g.number_of_nodes() == 0:
        return {}
    ws = [w for _, _, w in g.edges(data="insway")]
    lo, hi = (min(ws), max(ws)) if ws else (0, 1)
    h = nx.Graph()
    h.add_nodes_from(g)
    for u, v, w in g.edges(data="insway"):
        h.add_edge(u, v, spring=0.2 + 0.8 * (w - lo) / ((hi - lo) or 1))
    pieces = sorted((sorted(c, key=str) for c in nx.connected_components(h)), key=lambda c: (-len(c), str(c[0])))
    linked = [c for c in pieces if len(c) > 1]
    single = [c[0] for c in pieces if len(c) == 1]
    pos = {}
    boxes = []  # (piece positions centred at 0, half-width)
    for c in linked:
        sub = h.subgraph(c)
        p = nx.spring_layout(sub, weight="spring", seed=seed, iterations=iterations,
                             k=2.0 / math.sqrt(len(c)))
        r = math.sqrt(len(c))
        p = nx.rescale_layout_dict(p, scale=r)
        boxes.append((p, r))
    width = 2.2 * boxes[0][1] * 2 if boxes else 10.0
    x = y = row_h = 0.0
    for p, r in boxes:
        if x > 0 and x + 2 * r > width:
            x, y, row_h = 0.0, y - row_h - 1.0, 0.0
        for v, (px, py) in p.items():
            pos[v] = (x + r + px, y - r + py)
        x += 2 * r + 1.0
        row_h = max(row_h, 2 * r)
    if single:
        y = y - row_h - 1.0
        per_row = max(1, int(width))
        for i, v in enumerate(single):
            pos[v] = (0.5 + (i % per_row), y - (i // per_row))
    return {v: (float(a), float(b)) for v, (a, b) in pos.items()}


def component_sizes(g):
    sizes = {}
    for _, c in g.nodes(data="component"):
        sizes[c] = sizes.get(c, 0) + 1
    return sizes


def _palette(n):
    """n distinct colours as hex strings (tab20, then golden-angle HSV)."""
    import colorsys
    tab20 = ["#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd", "#8c564b", "#e377c2", "#7f7f7f",
             "#bcbd22", "#17becf", "#aec7e8", "#ffbb78", "#98df8a", "#ff9896", "#c5b0d5", "#c49c94",
             "#f7b6d2", "#c7c7c7", "#dbdb8d", "#9edae5"]
    out = []
    for i in range(n):
        if i < len(tab20):
            out.append(tab20[i])
        else:
            r, gg, b = colorsys.hsv_to_rgb((i * 0.618033988749895) % 1.0, 0.55, 0.85)
            out.append("#%02x%02x%02x" % (int(r * 255), int(gg * 255), int(b * 255)))
    return out


def node_colours(g):
    """Map component -> colour; largest components get the first colours, isolated objects grey."""
    sizes = component_sizes(g)
    comps = sorted((c for c in sizes if c >= 0), key=lambda c: (-sizes[c], c))
    colours = dict(zip(comps, _palette(len(comps))))
    colours[-1] = "#d0d0d0"
    return colours


def edge_widths(g, lo=0.5, hi=4.0):
    ws = [w for _, _, w in g.edges(data="insway")]
    if not ws:
        return {}
    a, b = min(ws), max(ws)
    span = (b - a) or 1
    return {(u, v): lo + (hi - lo) * (w - a) / span for u, v, w in g.edges(data="insway")}


def draw_static(g, pos, out, title=None, labels=True, figsize=10.0, dpi=150):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    colours = node_colours(g)
    widths = edge_widths(g)
    xs = [p[0] for p in pos.values()] or [0.0]
    ys = [p[1] for p in pos.values()] or [0.0]
    aspect = ((max(ys) - min(ys)) or 1) / ((max(xs) - min(xs)) or 1)
    fig, ax = plt.subplots(figsize=(figsize, max(2.0, min(figsize, figsize * aspect + 0.6))))
    edgelist = list(g.edges())
    nx.draw_networkx_edges(g, pos, ax=ax, edgelist=edgelist, width=[widths[e] for e in edgelist],
                           edge_color="#555555", alpha=0.6)
    nodelist = list(g.nodes())
    nx.draw_networkx_nodes(g, pos, ax=ax, nodelist=nodelist, node_size=60,
                           node_color=[colours[g.nodes[v]["component"]] for v in nodelist],
                           edgecolors="#333333", linewidths=0.4)
    if labels:
        nx.draw_networkx_labels(g, pos, ax=ax, font_size=6)
    ax.set_aspect("equal")
    ax.set_axis_off()
    if title:
        ax.set_title(title, fontsize=10)
    fig.tight_layout()
    fig.savefig(out, dpi=dpi)
    plt.close(fig)


def write_html(g, pos, out, title="linkage graph", size=900):
    """Self-contained HTML page: inline SVG, hover shows object, component and in-sway."""
    colours = node_colours(g)
    widths = edge_widths(g)
    pad = 30
    xs = [p[0] for p in pos.values()] or [0.0]
    ys = [p[1] for p in pos.values()] or [0.0]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    sx = sy = (size - 2 * pad) / (max(x1 - x0, y1 - y0) or 1)

    def xy(v):
        return pad + (pos[v][0] - x0) * sx, pad + (y1 - pos[v][1]) * sy

    sizes = component_sizes(g)
    parts = []
    for u, v, w in sorted(g.edges(data="insway"), key=lambda e: (str(e[0]), str(e[1]))):
        (ax, ay), (bx, by) = xy(u), xy(v)
        parts.append('<line x1="%.1f" y1="%.1f" x2="%.1f" y2="%.1f" stroke-width="%.2f"><title>%s - %s: in-sway %d'
                     '</title></line>' % (ax, ay, bx, by, widths[(u, v)], html.escape(str(u)), html.escape(str(v)), w))
    for v in sorted(g.nodes(), key=str):
        c = g.nodes[v]["component"]
        x, y = xy(v)
        comp = "isolated" if c < 0 else "component %d (%d objects)" % (c, sizes[c])
        parts.append('<g><circle cx="%.1f" cy="%.1f" r="5" fill="%s"/><text x="%.1f" y="%.1f">%s</text>'
                     '<title>%s: %s</title></g>' % (x, y, colours[c], x + 6, y - 6, html.escape(str(v)),
                                                    html.escape(str(v)), comp))
    page = """<!doctype html>
<html><head><meta charset="utf-8"><title>%s</title>
<style>
body { font-family: sans-serif; margin: 1em; }
svg { border: 1px solid #ccc; max-width: 100%%; height: auto; }
line { stroke: #555; stroke-opacity: 0.6; }
circle { stroke: #333; stroke-width: 0.5; }
text { font-size: 9px; fill: #222; pointer-events: none; }
#hidelabels:checked ~ svg text { display: none; }
</style></head><body>
<h3>%s</h3>
<p>%d objects, %d links, %d components with at least one link. Hover for details.</p>
<input type="checkbox" id="hidelabels"><label for="hidelabels"> hide labels</label><br>
<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">
%s
</svg>
</body></html>
""" % (html.escape(title), html.escape(title), g.number_of_nodes(), g.number_of_edges(),
       len([c for c in sizes if c >= 0]), size, size, size, size, "\n".join(parts))
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write(page)


def summary(g):
    sizes = component_sizes(g)
    linked = sorted((s for c, s in sizes.items() if c >= 0), reverse=True)
    ws = [w for _, _, w in g.edges(data="insway")]
    return {
        "objects": g.number_of_nodes(),
        "links": g.number_of_edges(),
        "components": len(linked),
        "largest_component": linked[0] if linked else 0,
        "isolated": sizes.get(-1, 0),
        "insway_min": min(ws) if ws else None,
        "insway_max": max(ws) if ws else None,
    }


def main(argv=None):
    p = argparse.ArgumentParser(description="Draw a force-directed embedding of rbl.jar linkage graph output.")
    p.add_argument("input", help="rbl.jar output folder, or a *-modest-edges.csv file")
    p.add_argument("-o", "--output", action="append", default=[],
                   help="output file; .png/.svg/.pdf drawn with matplotlib, .html written as interactive SVG. "
                        "May be repeated.")
    p.add_argument("--singletons", help="singletons CSV (found automatically when input is a folder)")
    p.add_argument("--no-singletons", action="store_true", help="do not draw isolated objects")
    p.add_argument("--seed", type=int, default=1, help="layout seed (default 1)")
    p.add_argument("--iterations", type=int, default=200, help="spring layout iterations (default 200)")
    p.add_argument("--no-labels", action="store_true", help="omit object labels in static output")
    p.add_argument("--title", help="figure title")
    args = p.parse_args(argv)

    edge_csv, single_csv = find_output_files(args.input)
    if args.singletons:
        single_csv = args.singletons
    if args.no_singletons:
        single_csv = None
    g = read_linkage_graph(edge_csv, single_csv)
    s = summary(g)
    print("%(objects)d objects, %(links)d links, %(components)d components (largest %(largest_component)d), "
          "%(isolated)d isolated; in-sway %(insway_min)s..%(insway_max)s" % s)
    if not args.output:
        return 0
    pos = layout(g, seed=args.seed, iterations=args.iterations)
    title = args.title or os.path.basename(os.path.dirname(os.path.abspath(edge_csv)))
    for out in args.output:
        if out.lower().endswith((".html", ".htm")):
            write_html(g, pos, out, title=title)
        else:
            draw_static(g, pos, out, title=title, labels=not args.no_labels)
        print("wrote " + out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
