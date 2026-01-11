CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS posts (
    -- id SERIAL PRIMARY KEY,
    id UUID DEFAULT uuid_generate_v4(),
    title VARCHAR(255),
    content VARCHAR(255),
    status VARCHAR(255) default 'DRAFT',
    attachment bytea,
    cover_image bytea,
    cover_image_thumbnail bytea,
    version INTEGER
);

ALTER TABLE posts ADD CONSTRAINT posts_pk PRIMARY KEY (id);