CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- there is no 'if not exists' for type
-- DROP TYPE post_status;
-- the value only accept single quotes.
-- CREATE TYPE post_status AS ENUM( 'DRAFT', 'PENDING_MODERATION', 'PUBLISHED');

DO $$ BEGIN
    CREATE TYPE post_status AS ENUM( 'DRAFT', 'PENDING_MODERATION', 'PUBLISHED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

CREATE CAST (varchar AS post_status) WITH INOUT AS IMPLICIT;