CREATE TABLE user_subscription {
chanel_id bigint NOT NULL REFERENCES usr(id),
subscriber_id bigint NOT NULL REFERENCES usr(id),
PRIMARY KEY (chanel_id, subscriber_id)
};