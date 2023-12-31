-- 23c supports `if not exists`
-- see: https://oracle-base.com/articles/23c/if-not-exists-ddl-clause-23c
CREATE SEQUENCE IF NOT EXISTS todos_seq START WITH 1;

CREATE TABLE IF NOT EXISTS todos
(
    id    NUMBER(10)  DEFAULT todos_seq.nextval NOT NULL,
    title VARCHAR2(200),
    PRIMARY KEY(id)
);

-- ALTER TABLE todos DROP CONSTRAINT IF EXISTS todos_pk;
-- see: https://gist.github.com/jarrodhroberson/067c8dd7dc40482cd984bb937f8c9f42
-- BEGIN
--     EXECUTE IMMEDIATE 'ALTER TABLE todos DROP CONSTRAINT todos_pk';
-- EXCEPTION
--   WHEN OTHERS THEN
--     IF SQLCODE != -2443 THEN
--       RAISE;
--     END IF;
-- END;
--
-- ALTER TABLE todos
--     ADD CONSTRAINT todos_pk PRIMARY KEY (id);
