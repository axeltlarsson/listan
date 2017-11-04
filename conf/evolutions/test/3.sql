# Items schema H2

# --- !Ups
CREATE TABLE items (
  uuid varchar(255) NOT NULL PRIMARY KEY,
  contents varchar(255) NOT NULL,
  completed boolean DEFAULT false,
  list_uuid varchar(255) REFERENCES lists(uuid) ON DELETE CASCADE,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP(),
  updated_at timestamp AS CURRENT_TIMESTAMP()
)

# --- !Downs
DROP TABLE items;
