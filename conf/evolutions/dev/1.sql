# Users schema H2

# --- !Ups
CREATE ALIAS UUID FOR
"org.h2.value.ValueUuid.getNewRandom";
CREATE TABLE users (
  uuid varchar(255) NOT NULL DEFAULT UUID() PRIMARY KEY,
  name varchar(255) NOT NULL,
  password_hash varchar(255) NOT NULL,
  created datetime DEFAULT CURRENT_TIMESTAMP(),
  updated datetime AS CURRENT_TIMESTAMP()
)

# --- !Downs
DROP TABLE users;
