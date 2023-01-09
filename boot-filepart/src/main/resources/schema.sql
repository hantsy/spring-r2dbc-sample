CREATE TABLE IF NOT EXISTS posts (
    -- id SERIAL PRIMARY KEY,
    id UUID DEFAULT uuid_generate_v4(),
    title VARCHAR(255),
    content VARCHAR(255),
    metadata JSON default '{}',
    -- In this sample, use Varchar to store enum(name), Spring Data R2dbc can convert Java Enum to pg VARCHAR, and reverse.
    status VARCHAR(255) default 'DRAFT',
    created_at TIMESTAMP , --NOT NULL DEFAULT LOCALTIMESTAMP,
    updated_at TIMESTAMP,
    attachment bytea,
    cover_image bytea,
    cover_image_thumbnail bytea,
    version INTEGER,
    PRIMARY KEY (id)
);

