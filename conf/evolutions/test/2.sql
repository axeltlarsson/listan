# Items schema H2

# --- !Ups
CREATE TABLE items (
  uuid varchar(255) NOT NULL PRIMARY KEY,
  contents varchar(255) NOT NULL,
  completed boolean DEFAULT false,
  created datetime DEFAULT CURRENT_TIMESTAMP(),
  updated datetime AS CURRENT_TIMESTAMP()
)

# --- !Downs
DROP TABLE items;
