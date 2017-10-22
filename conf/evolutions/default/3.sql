# Items schema

# --- !Ups
CREATE TABLE items (
  uuid text NOT NULL PRIMARY KEY,
  contents text NOT NULL,
  completed boolean DEFAULT false,
  list_uuid text NOT NULL REFERENCES lists ON DELETE CASCADE,
  created timestamp DEFAULT now(),
  updated timestamp DEFAULT now()
);

CREATE TRIGGER items_updated_timestamp
  BEFORE UPDATE ON items
  FOR EACH ROW EXECUTE PROCEDURE set_updated_timestamp();


# --- !Downs
DROP TABLE items;

