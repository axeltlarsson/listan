# Items schema

# --- !Ups
CREATE TABLE items (
  uuid text NOT NULL PRIMARY KEY,
  contents text NOT NULL,
  completed boolean DEFAULT false,
  list_uuid text REFERENCES lists(uuid) ON DELETE CASCADE,
  created_at timestamp DEFAULT now(),
  updated_at timestamp DEFAULT now()
);

CREATE TRIGGER items_updated_at_timestamp
  BEFORE UPDATE ON items
  FOR EACH ROW EXECUTE PROCEDURE set_updated_at_timestamp();


# --- !Downs
DROP TABLE items;

