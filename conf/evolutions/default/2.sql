# Lists schema

# --- !Ups
CREATE TABLE lists (
  uuid text NOT NULL PRIMARY KEY,
  name text NOT NULL UNIQUE,
  description text,
  user_uuid text REFERENCES users ON DELETE CASCADE,
  created timestamp DEFAULT now(),
  updated timestamp DEFAULT now()
);

CREATE TRIGGER lists_updated_timestamp
  BEFORE UPDATE ON lists
  FOR EACH ROW EXECUTE PROCEDURE set_updated_timestamp();


# --- !Downs
DROP TABLE items;

