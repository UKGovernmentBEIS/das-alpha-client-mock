# --- !Ups

INSERT INTO DAS_USER (ID, NAME, PASSWORD) VALUES (1, 'doug', 'password');

# --- !Downs

DELETE FROM DAS_USER;
