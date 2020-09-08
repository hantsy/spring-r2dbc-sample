CREATE TABLE IF NOT EXISTS posts (
    -- id SERIAL PRIMARY KEY,
    id UUID DEFAULT uuid_generate_v4(),
    title VARCHAR(255),
    content VARCHAR(255),
    metadata JSON default '{}',

    -- Spring data r2dbc can convert Java Enum to pg VARCHAR, and reverse.
    status VARCHAR(255) default 'DRAFT',
    created_at TIMESTAMP , --NOT NULL DEFAULT LOCALTIMESTAMP,
    updated_at TIMESTAMP,
    version INTEGER,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS comments (
    id UUID DEFAULT uuid_generate_v4(),
    content VARCHAR(255),
    post_id UUID REFERENCES posts ON DELETE CASCADE,
    created_at TIMESTAMP ,
    updated_at TIMESTAMP,
    version INTEGER,
    PRIMARY KEY (id)
);
