CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS posts
(
    id         UUID         DEFAULT uuid_generate_v4(),
    title      VARCHAR(255),
    content    VARCHAR(255),
    metadata   JSON         default '{}',
    statistics JSONB,
    -- Spring data r2dbc can convert Java Enum to pg VARCHAR, and reverse.
    status     VARCHAR(255) default 'DRAFT',
    version    INTEGER
);

ALTER TABLE posts ADD CONSTRAINT posts_pk PRIMARY KEY (id);