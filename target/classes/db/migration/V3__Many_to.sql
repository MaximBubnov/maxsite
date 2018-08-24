create table user_subscription(
chanel_id bigint not null,
subscriber_id bigint not null,
PRIMARY KEY (chanel_id, subscriber_id)
);

alter table user_subscription add constraint user_subscription_chanel_id_fk foreign key (chanel_id) references usr (id);
alter table user_subscription add constraint user_subscription_subscriber_id_fk foreign key (subscriber_id) references usr (id);