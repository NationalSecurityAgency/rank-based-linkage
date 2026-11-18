import csv
import os
import sys

import pytest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))

import rbl_visualize as rv  # noqa: E402

FIXTURE = os.path.join(HERE, "fixtures", "migrantFlows-Dh-8")


def rows(path):
    with open(path, newline="") as f:
        return list(csv.reader(f))[1:]


def test_find_output_files():
    edges, singles = rv.find_output_files(FIXTURE)
    assert edges.endswith("-modest-edges.csv")
    assert singles is not None and "-singletons-among-" in singles


def test_graph_matches_csv():
    edges, singles = rv.find_output_files(FIXTURE)
    g = rv.read_linkage_graph(edges, singles)
    e_rows = rows(edges)
    s_rows = rows(singles)
    assert g.number_of_edges() == len(e_rows)
    linked = {r[1] for r in e_rows} | {r[2] for r in e_rows}
    assert g.number_of_nodes() == len(linked) + len(s_rows)
    for c, s, t, w in e_rows:
        assert g[s][t]["insway"] == int(w)
        assert g.nodes[s]["component"] == g.nodes[t]["component"] == int(c)
    for (v,) in s_rows:
        assert g.nodes[v]["component"] == -1


def test_components_are_connected_pieces():
    """Every component number written by rbl.jar is one connected piece of the linkage subgraph."""
    edges, _ = rv.find_output_files(FIXTURE)
    g = rv.read_linkage_graph(edges)
    import networkx as nx
    for piece in nx.connected_components(g):
        assert len({g.nodes[v]["component"] for v in piece}) == 1
    assert len(list(nx.connected_components(g))) == rv.summary(g)["components"]


def test_layout_is_deterministic():
    g = rv.read_linkage_graph(*rv.find_output_files(FIXTURE))
    a = rv.layout(g, seed=7)
    b = rv.layout(g, seed=7)
    assert a.keys() == b.keys()
    for v in a:
        assert tuple(a[v]) == tuple(b[v])


def test_bad_header(tmp_path):
    p = tmp_path / "x.csv"
    p.write_text("a,b,c\n1,2,3\n")
    with pytest.raises(ValueError):
        rv.read_linkage_graph(str(p))


def test_cli_writes_outputs(tmp_path, capsys):
    png, svg, page = tmp_path / "g.png", tmp_path / "g.svg", tmp_path / "g.html"
    assert rv.main([FIXTURE, "-o", str(png), "-o", str(svg), "-o", str(page)]) == 0
    assert png.read_bytes()[:8] == b"\x89PNG\r\n\x1a\n"
    assert b"<svg" in svg.read_bytes()
    text = page.read_text(encoding="utf-8")
    g = rv.read_linkage_graph(*rv.find_output_files(FIXTURE))
    assert text.count("<circle") == g.number_of_nodes()
    assert text.count("<line") == g.number_of_edges()
    assert "<script" not in text
    out = capsys.readouterr().out
    assert "%d objects, %d links" % (g.number_of_nodes(), g.number_of_edges()) in out


def test_html_is_reproducible(tmp_path):
    a, b = tmp_path / "a.html", tmp_path / "b.html"
    rv.main([FIXTURE, "-o", str(a), "--seed", "3"])
    rv.main([FIXTURE, "-o", str(b), "--seed", "3"])
    assert a.read_bytes() == b.read_bytes()
