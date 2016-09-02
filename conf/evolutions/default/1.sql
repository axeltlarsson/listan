# Users schema

# --- !Ups
CREATE TABLE USER (
  uuid varchar(255) NOT NULL PRIMARY KEY,
  user_name varchar(255) NOT NULL,
  password varchar(255) NOT NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP(),
  updated_at datetime AS CURRENT_TIMESTAMP()
)

# --- !Downs
DROP TABLE USER;
