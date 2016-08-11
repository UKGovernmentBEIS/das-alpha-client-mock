# --- !Ups

CREATE SEQUENCE "transient_access_token_seq" INCREMENT BY 1 NO MAXVALUE NO MINVALUE START 100000 CACHE 1;

CREATE TABLE "transient_access_token_details" (
  "id"            BIGINT  NOT NULL PRIMARY KEY DEFAULT nextval('transient_access_token_seq' :: REGCLASS),
  "empref"        VARCHAR NOT NULL,
  "user_id"       BIGINT  NOT NULL REFERENCES "das_user",
  "access_token"  VARCHAR NOT NULL,
  "valid_until"   BIGINT  NOT NULL,
  "refresh_token" VARCHAR NOT NULL
);

# --- !Downs

DROP TABLE "transient_access_token_details";

DROP SEQUENCE "transient_access_token_seq"