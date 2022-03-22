CREATE TABLE IF NOT EXISTS posts (
    id UUID NOT NULL /* [jooq ignore start] */ DEFAULT uuid_generate_v4()/* [jooq ignore stop] */,
    title VARCHAR(255),
    content VARCHAR(255),
    status VARCHAR(255) default 'DRAFT',
    cups_of_coffee NUMERIC[],
    created_at TIMESTAMP , --NOT NULL DEFAULT LOCALTIMESTAMP,
    version INTEGER,
    PRIMARY KEY (id)
);