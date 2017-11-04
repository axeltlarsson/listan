# Lists schema

# --- !Ups
CREATE TABLE lists (
  uuid varchar(255) NOT NULL PRIMARY KEY,
  name varchar(255) NOT NULL UNIQUE,
  description varchar(255),
  user_uuid varchar(255) REFERENCES users(uuid) ON DELETE CASCADE,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP(),
  updated_at timestamp AS CURRENT_TIMESTAMP()
);

# --- !Downs
DROP TABLE lists;

