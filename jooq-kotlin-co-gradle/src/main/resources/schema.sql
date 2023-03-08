CREATE TABLE IF NOT EXISTS posts (
    id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
    title VARCHAR(255),
    content VARCHAR(255),
    status VARCHAR(255) default 'DRAFT',
    tags VARCHAR(255)[],
    created_at TIMESTAMP,
    version INTEGER,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS comments (
    id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4() /* [jooq ignore stop] */,
    content VARCHAR(255),
    post_id UUID REFERENCES posts  /* [jooq ignore start] */  ON DELETE CASCADE /* [jooq ignore stop] */,
    created_at TIMESTAMP ,
    PRIMARY KEY (id)
);