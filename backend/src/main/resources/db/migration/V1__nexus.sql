CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE incident (
 id uuid PRIMARY KEY, service varchar(40) NOT NULL, status varchar(40) NOT NULL,
 opened_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 evidence jsonb NOT NULL, diagnosis jsonb, validation jsonb, outcome jsonb,
 approved_by varchar(100), approved_at timestamptz, version int NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX one_active_incident ON incident(service) WHERE status NOT IN ('RESOLVED','CLOSED');
CREATE TABLE audit (
 id bigserial PRIMARY KEY, incident_id uuid REFERENCES incident(id), actor varchar(100) NOT NULL,
 event varchar(80) NOT NULL, detail jsonb NOT NULL, at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE memory (
 id uuid PRIMARY KEY REFERENCES incident(id), content text NOT NULL,
 embedding vector(1536), embedding_model varchar(120), created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX memory_vector_idx ON memory USING hnsw (embedding vector_cosine_ops);
CREATE TABLE sample (
 id bigserial PRIMARY KEY, service varchar(40) NOT NULL, at timestamptz NOT NULL DEFAULT now(), data jsonb NOT NULL
);
CREATE INDEX sample_time_idx ON sample(at);
