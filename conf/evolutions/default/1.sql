# Users schema MySQL

# --- !Ups
CREATE TABLE users (
  uuid varchar(255) NOT NULL PRIMARY KEY,
  name varchar(255) NOT NULL,
  password_hash varchar(255) NOT NULL,
  created datetime DEFAULT CURRENT_TIMESTAMP,
  updated datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

# --- !Downs
DROP TABLE users;
