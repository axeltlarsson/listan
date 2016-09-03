# Items schema H2

# --- !Ups
CREATE TABLE items (
  uuid varchar(255) NOT NULL DEFAULT UUID() PRIMARY KEY,
  contents varchar(255) NOT NULL,
  completed boolean DEFAULT false,
  created datetime DEFAULT CURRENT_TIMESTAMP(),
  updated datetime AS CURRENT_TIMESTAMP()
)

INSERT INTO items (contents) VALUES ('item1');
INSERT INTO items (contents) VALUES ('item2');

# --- !Downs
DROP TABLE items;
