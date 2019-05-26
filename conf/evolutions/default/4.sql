# Remove unique constraint lists(name), add unique name per user_uuid

# --- !Ups
ALTER TABLE lists DROP CONSTRAINT lists_name_key;
ALTER TABLE lists ADD CONSTRAINT lists_unique_name_per_user UNIQUE (name, user_uuid);

# --- !Downs
ALTER TABLE lists DROP CONSTRAINT lists_unique_name_per_user;
ALTER TABLE lists ADD CONSTRAINT lists_name_key UNIQUE (name);

