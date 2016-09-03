# will insert (axel, whatever)
# --- !Ups
INSERT INTO users (name, password_hash) VALUES ('axel', '$2a$10$ZWwJdapgoAGSbAwmPCL7detqRCn78LJQW.L8I81gLv3Kz09N6FNly');

# --- !Downs
DELETE FROM users WHERE name='axel';
