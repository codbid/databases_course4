# Минимальный экспортер метрик Neo4j для Community Edition (нет встроенного Prometheus).
FROM python:3.11-slim
RUN pip install --no-cache-dir neo4j prometheus-client
WORKDIR /app
COPY exporter.py .
EXPOSE 5000
ENV NEO4J_URI=bolt://neo4j:7687
ENV NEO4J_USER=neo4j
ENV NEO4J_PASSWORD=password
CMD ["python", "exporter.py"]
