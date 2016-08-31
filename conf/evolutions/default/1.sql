# Users schema

# --- !Ups
CREATE TABLE User (
  id bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  password varchar(255) NOT NULL,
  name varchar(255) NOT NULL
)

# --- !Downs
DROP TABLE User;
