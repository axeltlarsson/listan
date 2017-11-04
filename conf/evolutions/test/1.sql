# Users schema H2

# --- !Ups
CREATE TABLE users (
  uuid varchar(255) NOT NULL PRIMARY KEY,
  name varchar(255) NOT NULL UNIQUE,
  password_hash varchar(255) NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP(),
  updated_at timestamp AS CURRENT_TIMESTAMP()
)

# --- !Downs
DROP TABLE users;
