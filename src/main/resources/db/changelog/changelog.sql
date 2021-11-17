--liquibase formatted sql
CREATE DATABASE config
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1;

--changeset liquibase:1
CREATE TABLE PROPERTIES (
  id            SERIAL      PRIMARY KEY,
  CREATED_ON    TIMESTAMP   NOT NULL DEFAULT NOW(),   -- Default on insert and update to now()
  APPLICATION   TEXT        NOT NULL,   -- Folder name
  PROFILE       TEXT        NOT NULL,   -- application-dev.yml (.split(“-”)[1].split(“.”)[0])
  LABEL         TEXT        NOT NULL,   -- Not sure about this “latest"
  PROP_KEY      TEXT        NOT NULL,   -- key
  VALUE         TEXT        NOT NULL    -- value
 );

INSERT INTO PROPERTIES (APPLICATION, PROFILE, LABEL, PROP_KEY, VALUE)
VALUES ('test', 'dev', 'test', 'test', 'val');