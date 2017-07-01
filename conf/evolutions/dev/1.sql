# Users schema H2

# --- !Ups
CREATE TABLE users (
  uuid varchar(255) NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
  name varchar(255) NOT NULL UNIQUE,
  password_hash varchar(255) NOT NULL,
  created datetime DEFAULT CURRENT_TIMESTAMP(),
  updated datetime AS CURRENT_TIMESTAMP()
)

# --- !Downs
DROP TABLE users;
