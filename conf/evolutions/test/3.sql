# Add support for multiple lists

# --- !Ups
CREATE TABLE lists (
  uuid varchar(255) NOT NULL DEFAULT UUID() PRIMARY KEY,
  name varchar(255) NOT NULL UNIQUE,
  description varchar(255),
  user_uuid varchar(255) NOT NULL,
  created datetime DEFAULT CURRENT_TIMESTAMP(),
  updated datetime AS CURRENT_TIMESTAMP(),
  FOREIGN KEY (user_uuid) REFERENCES users(uuid)
);

ALTER TABLE items
    ADD list_uuid varchar(255) NOT NULL;

ALTER TABLE items
    ADD FOREIGN KEY (list_uuid)
        REFERENCES lists(uuid)
        ON DELETE CASCADE;

# --- !Downs
DROP TABLE lists;

ALTER TABLE items
    DROP COLUMN list_uuid;
