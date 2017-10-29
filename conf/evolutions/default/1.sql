# Users schema

# --- !Ups
CREATE TABLE users (
  uuid text NOT NULL PRIMARY KEY,
  name text UNIQUE NOT NULL,
  password_hash text NOT NULL,
  created timestamp DEFAULT now(),
  updated timestamp DEFAULT now()
);

CREATE UNIQUE INDEX users_name_idx ON users (name);

CREATE OR REPLACE FUNCTION set_updated_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated = now();;
    RETURN NEW;;
END;;
$$ language 'plpgsql';

CREATE OR REPLACE FUNCTION set_uuid()
RETURNS TRIGGER AS $$
BEGIN
    NEW.uuid = 'uuid-very-real-not-fake-at-all';;
    RETURN NEW;;
END;;
$$ language 'plpgsql';

CREATE TRIGGER users_updated_timestamp
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE PROCEDURE set_updated_timestamp();

CREATE TRIGGER users_set_uuid
BEFORE INSERT ON users
  FOR EACH ROW EXECUTE PROCEDURE set_uuid();

# --- !Downs
DROP TABLE users;
DROP FUNCTION set_updated_timestamp;
