# Implementation references

The source code pins a Spring Boot 3.5 / Spring AI 1.1 combination. Spring AI's maintained repository documents the Boot compatibility branches and its chat/vector abstractions: [Spring AI repository](https://github.com/spring-projects/spring-ai).

Implementation API references: [Spring AI ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html), [PGvector integration concepts](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html). NEXUS uses Spring AI embeddings with parameterized PGvector SQL through Spring JDBC; it does not claim to instantiate Spring AI's PgVectorStore abstraction.

The academic bibliography in `academic-report.md` is transcribed from the presentation and is separate from these implementation references.
