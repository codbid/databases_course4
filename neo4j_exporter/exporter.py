"""Prometheus exporter for Neo4j Community: node/relationship counts and basic stats."""
import os
from http.server import HTTPServer, BaseHTTPRequestHandler
from neo4j import GraphDatabase
from prometheus_client import REGISTRY, Gauge, generate_latest, CONTENT_TYPE_LATEST

NEO4J_URI = os.environ.get("NEO4J_URI", "bolt://neo4j:7687")
NEO4J_USER = os.environ.get("NEO4J_USER", "neo4j")
NEO4J_PASSWORD = os.environ.get("NEO4J_PASSWORD", "password")

# Gauges
neo4j_nodes = Gauge("neo4j_nodes_total", "Total number of nodes in the default database")
neo4j_relationships = Gauge("neo4j_relationships_total", "Total number of relationships")
neo4j_up = Gauge("neo4j_up", "1 if Neo4j is reachable, 0 otherwise")


def collect():
    driver = None
    try:
        driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD))
        with driver.session() as session:
            r = session.run("MATCH (n) RETURN count(n) AS c")
            neo4j_nodes.set(r.single()["c"])
            r = session.run("MATCH ()-[r]->() RETURN count(r) AS c")
            neo4j_relationships.set(r.single()["c"])
        neo4j_up.set(1)
    except Exception:
        neo4j_up.set(0)
        neo4j_nodes.set(0)
        neo4j_relationships.set(0)
    finally:
        if driver:
            driver.close()


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/metrics":
            collect()
            self.send_response(200)
            self.send_header("Content-Type", CONTENT_TYPE_LATEST)
            self.end_headers()
            self.wfile.write(generate_latest(REGISTRY))
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        pass


if __name__ == "__main__":
    port = int(os.environ.get("PORT", "5000"))
    HTTPServer(("0.0.0.0", port), Handler).serve_forever()
