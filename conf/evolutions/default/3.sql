# Items schema

# --- !Downs
CREATE TABLE items (
  uuid text NOT NULL PRIMARY KEY,
  contents text NOT NULL,
  completed boolean DEFAULT false,
  list_uuid text NOT NULL REFERENCES lists ON DELETE CASCADE,
  created TIMESTAMP DEFAULT now(),
  updated TIMESTAMP DEFAULT now()
);

CREATE TRIGGER items_updated_timestamp
  BEFORE UPDATE ON items
  FOR EACH ROW EXECUTE PROCEDURE set_updated_timestamp();


# --- !Downs
DROP TABLE items;

