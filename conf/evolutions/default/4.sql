#-- Add unique constraint to user name

# --- !Ups
ALTER TABLE users
    ADD CONSTRAINT unique_name UNIQUE (name);

# --- !Downs
ALTER TABLE users
    DROP INDEX  unique_name;
