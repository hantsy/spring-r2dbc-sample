CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS posts (
-- id SERIAL PRIMARY KEY,
    id UUID DEFAULT uuid_generate_v4(),
    title VARCHAR(255),
    content VARCHAR(255),
    status VARCHAR(255) default 'DRAFT',
    version INTEGER
 );

 CREATE TABLE IF NOT EXISTS post_logs (
     id UUID DEFAULT uuid_generate_v4(),
     entity_id UUID,
     entity_type VARCHAR(255),
     snapshot JSON default '{}',
     created_by VARCHAR(255),
     created_at TIMESTAMP ,
     version INTEGER
  );
  
ALTER TABLE posts ADD CONSTRAINT posts_pk PRIMARY KEY (id);
ALTER TABLE post_logs ADD CONSTRAINT post_logs_pk PRIMARY KEY (id);