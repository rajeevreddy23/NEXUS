CREATE TABLE app_user (
 id uuid PRIMARY KEY, username varchar(80) NOT NULL UNIQUE, password varchar(120) NOT NULL,
 role varchar(20) NOT NULL CHECK(role IN ('ADMIN','MEMBER')), enabled boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE project (
 id uuid PRIMARY KEY, name varchar(160) NOT NULL, description text NOT NULL DEFAULT '',
 owner_id uuid NOT NULL REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 version int NOT NULL DEFAULT 0, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE project_member (
 project_id uuid REFERENCES project(id), user_id uuid REFERENCES app_user(id), PRIMARY KEY(project_id,user_id)
);
CREATE TABLE task (
 id uuid PRIMARY KEY, project_id uuid NOT NULL REFERENCES project(id), title varchar(200) NOT NULL,
 description text NOT NULL DEFAULT '', status varchar(20) NOT NULL DEFAULT 'TODO' CHECK(status IN ('TODO','IN_PROGRESS','DONE')),
 priority varchar(10) NOT NULL CHECK(priority IN ('LOW','MEDIUM','HIGH')), assignee_id uuid REFERENCES app_user(id),
 due_date date, created_by uuid NOT NULL REFERENCES app_user(id), version int NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX task_project_idx ON task(project_id,updated_at DESC);
CREATE TABLE business_audit (
 id bigserial PRIMARY KEY, actor varchar(100) NOT NULL, action varchar(80) NOT NULL,
 entity_id uuid, detail text NOT NULL, at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE operational_state (
 id int PRIMARY KEY CHECK(id=1), maintenance boolean NOT NULL DEFAULT false,
 reason varchar(500) NOT NULL DEFAULT '', version int NOT NULL DEFAULT 0, updated_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO operational_state(id) VALUES(1);
