# --- !Ups

CREATE TABLE "das_user" (
  "id" INTEGER NOT NULL PRIMARY KEY,
  "name" VARCHAR NOT NULL,
  "password" VARCHAR NOT NULL
);

INSERT INTO "das_user" ("id", "name", "password") VALUES (1, 'doug', 'password');

CREATE TABLE "scheme_claim" (
  "empref" VARCHAR NOT NULL UNIQUE,
  "das_user_id" INTEGER NOT NULL REFERENCES "das_user",
  "access_token" VARCHAR NOT NULL,
  "valid_until" DATE NOT NULL,
  "refresh_token" VARCHAR NULL
);

# --- !Downs

DROP TABLE "scheme_claim";
DROP TABLE "das_user";