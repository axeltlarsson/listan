# Items schema mysql

# --- !Ups
CREATE TABLE items (
  uuid varchar(255) NOT NULL PRIMARY KEY,
  contents varchar(255) NOT NULL,
  completed boolean DEFAULT false,
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

# --- !Downs
DROP TABLE items;

