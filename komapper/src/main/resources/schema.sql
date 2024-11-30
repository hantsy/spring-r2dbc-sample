CREATE TABLE IF NOT EXISTS posts
(
    id         BIGSERIAL,
    title      VARCHAR(255)  NOT NULL,
    body       VARCHAR(1000) NOT NULL,
    status     VARCHAR(10)   NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP,                              -- NOT NULL DEFAULT LOCALTIMESTAMP,
    updated_at TIMESTAMP,
    version    BIGINT
);

ALTER TABLE posts
    DROP CONSTRAINT IF EXISTS pk_posts;
ALTER TABLE posts
    ADD CONSTRAINT pk_posts PRIMARY KEY (id);