CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE post_status AS ENUM ('DRAFT', 'PENDING_MODERATION', 'PUBLISHED');

CREATE TABLE IF NOT EXISTS posts (
     id UUID DEFAULT uuid_generate_v4(),
     title VARCHAR(255),
     content VARCHAR(255),
     metadata JSON default '{}',
     status post_status default 'DRAFT',
     version INTEGER
 );

ALTER TABLE posts ADD CONSTRAINT posts_pk PRIMARY KEY (id);