# Add support for multiple lists

# --- !Ups
CREATE TABLE lists (
  uuid varchar(255) NOT NULL PRIMARY KEY,
  name varchar(255) NOT NULL UNIQUE,
  description varchar(255),
  user_uuid varchar(255) NOT NULL,
  created datetime DEFAULT CURRENT_TIMESTAMP,
  updated datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_uuid) REFERENCES users(uuid)
);

CREATE TRIGGER before_insert_list
  BEFORE INSERT ON lists
  FOR EACH ROW
  SET new.uuid = uuid();

ALTER TABLE items
    ADD list_uuid varchar(255) NOT NULL;

ALTER TABLE items
    ADD FOREIGN KEY (list_uuid) REFERENCES lists(uuid);

# --- !Downs
DROP TABLE lists;

ALTER TABLE items
    DROP FOREIGN KEY list_uuid;
ALTER TABLE items
    DROP COLUMN list_uuid;
