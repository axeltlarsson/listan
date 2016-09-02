# Users schema

# --- !Ups
CREATE TABLE USER (
  uuid varchar(255) NOT NULL PRIMARY KEY,
  name varchar(255) NOT NULL,
  password_hash varchar(255) NOT NULL,
  created datetime DEFAULT CURRENT_TIMESTAMP,
  updated datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)

CREATE TRIGGER before_insert_user
  BEFORE INSERT ON USER
  FOR EACH ROW
  SET new.uuid = uuid();


# --- !Downs
DROP TABLE USER;
