# Users schema

# --- !Ups
CREATE TABLE users (
  uuid text NOT NULL PRIMARY KEY,
  name text UNIQUE NOT NULL,
  password_hash text NOT NULL,
  created_at timestamp DEFAULT now(),
  updated_at timestamp DEFAULT now()
);

CREATE UNIQUE INDEX users_name_idx ON users (name);

CREATE OR REPLACE FUNCTION set_updated_at_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();;
    RETURN NEW;;
END;;
$$ language 'plpgsql';

CREATE TRIGGER users_updated_at_timestamp
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE PROCEDURE set_updated_at_timestamp();

# --- !Downs
DROP TABLE users;
DROP FUNCTION set_updated_at_timestamp();
