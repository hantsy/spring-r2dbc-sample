CREATE TABLE IF NOT EXISTS posts (
    id UUID DEFAULT uuid_generate_v4(),
    title VARCHAR(255),
    content VARCHAR(255),
    status VARCHAR(255) default 'DRAFT',
    created_at TIMESTAMP , --NOT NULL DEFAULT LOCALTIMESTAMP,
    version INTEGER,
    PRIMARY KEY (id)
);