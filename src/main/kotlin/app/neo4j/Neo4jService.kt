package com.example.app.neo4j

import com.example.config.Neo4jFactory
import org.neo4j.driver.Values

object Neo4jService {

    fun getGraphStats(): GraphStatsResponse {
        Neo4jFactory.driver().session().use { session ->
                val nodes = session.run("MATCH (n) RETURN count(n) AS c").single()["c"].asLong()
                val rels = session.run("MATCH ()-[r]->() RETURN count(r) AS c").single()["c"].asLong()
            return GraphStatsResponse(nodes = nodes, relationships = rels)
        }
    }

    fun getSimilarReaders(clientId: String): List<SimilarReaderResponse> {
        Neo4jFactory.driver().session().use { session ->
                val result = session.run(
                    """
                    MATCH (c1:Client {id: \$clientId})-[:BORROWED]->(copy:BookCopy)<-[:BORROWED]-(c2:Client)
                    WHERE c1 <> c2
                    WITH c2, count(copy) AS commonCopies
                    ORDER BY commonCopies DESC
                    LIMIT 10
                    RETURN c2.id AS id, c2.name AS name, commonCopies
                    """.trimIndent(),
                    Values.parameters("clientId", clientId)
                )
            return result.list().map { r ->
                SimilarReaderResponse(
                    id = r.get("id").asString(),
                    name = r.get("name").asString(),
                    commonBorrowedCopies = r.get("commonCopies").asLong()
                )
            }
        }
    }
}

data class GraphStatsResponse(val nodes: Long, val relationships: Long)
data class SimilarReaderResponse(val id: String, val name: String, val commonBorrowedCopies: Long)
