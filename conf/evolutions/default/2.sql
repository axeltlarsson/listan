# Lists schema

# --- !Ups
CREATE TABLE lists (
  uuid text NOT NULL PRIMARY KEY,
  name text NOT NULL UNIQUE,
  description text,
  user_uuid text REFERENCES users(uuid) ON DELETE CASCADE,
  created_at timestamp DEFAULT now(),
  updated_at timestamp DEFAULT now()
);

CREATE TRIGGER lists_updated_at_timestamp
  BEFORE UPDATE ON lists
  FOR EACH ROW EXECUTE PROCEDURE set_updated_at_timestamp();


# --- !Downs
DROP TABLE lists;

