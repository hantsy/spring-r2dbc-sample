CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE IF NOT EXISTS books
(
    id          UUID DEFAULT uuid_generate_v4(),
    isbn        VARCHAR(255),
    title       VARCHAR(255),
    description VARCHAR(255),
    tags        VARCHAR(255)[],
    metadata    JSON DEFAULT '{}',
    created_at  TIMESTAMP, --NOT NULL DEFAULT LOCALTIMESTAMP,
    updated_at  TIMESTAMP,
    version     INTEGER
);

CREATE TABLE IF NOT EXISTS reviews
(
    id         UUID         DEFAULT uuid_generate_v4(),
    content    VARCHAR(255),
    status     VARCHAR(255) default 'DRAFT',
    book_id    UUID NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    version    INTEGER
);

CREATE TABLE IF NOT EXISTS authors
(
    id             UUID DEFAULT uuid_generate_v4(),
    first_name     VARCHAR(255) NOT NULL,
    last_name      VARCHAR(255),
    address_street VARCHAR(255),
    city           VARCHAR(255),
    zipcode        VARCHAR(255),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    version        INTEGER
);

CREATE TABLE IF NOT EXISTS book_authors
(
    id        UUID DEFAULT uuid_generate_v4(),
    book_id   UUID NOT NULL,
    author_id UUID NOT NULL
);

-- drop foreign keys
ALTER TABLE reviews
    DROP CONSTRAINT IF EXISTS fk_reviews_to_books;
ALTER TABLE book_authors
    DROP CONSTRAINT IF EXISTS fk_book_authors_to_books;
ALTER TABLE book_authors
    DROP CONSTRAINT IF EXISTS fk_book_authors_to_authors;
-- drop primary keys
ALTER TABLE books
    DROP CONSTRAINT IF EXISTS pk_books;
ALTER TABLE reviews
    DROP CONSTRAINT IF EXISTS pk_reviews;
ALTER TABLE authors
    DROP CONSTRAINT IF EXISTS pk_authors;
ALTER TABLE book_authors
    DROP CONSTRAINT IF EXISTS pk_book_authors;

-- add primary keys
ALTER TABLE books
    ADD CONSTRAINT pk_books PRIMARY KEY (id);
ALTER TABLE reviews
    ADD CONSTRAINT pk_reviews PRIMARY KEY (id);
ALTER TABLE authors
    ADD CONSTRAINT pk_authors PRIMARY KEY (id);
ALTER TABLE book_authors
    ADD CONSTRAINT pk_book_authors PRIMARY KEY (id);

-- add foreign keys
ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_to_books FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE;
ALTER TABLE book_authors
    ADD CONSTRAINT fk_book_authors_to_books FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE;
ALTER TABLE book_authors
    ADD CONSTRAINT fk_book_authors_to_authors FOREIGN KEY (author_id) REFERENCES authors (id) ON DELETE CASCADE;