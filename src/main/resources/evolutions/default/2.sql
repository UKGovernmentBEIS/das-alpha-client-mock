# --- !Ups

CREATE TABLE "token_stash" (
  "empref"        VARCHAR NOT NULL,
  "access_token"  VARCHAR NOT NULL,
  "valid_until"   BIGINT  NOT NULL,
  "refresh_token" VARCHAR NOT NULL,
  "ref"           BIGINT  NOT NULL,
  "user_id"       BIGINT  NOT NULL REFERENCES "das_user"
);

# --- !Downs

DROP SEQUENCE "token_stash"