create table "user" (
                      id serial primary key,
                      "type" varchar(50) not null,
                      code varchar(32) not null,
                      first_name varchar(255) not null,
                      middle_name varchar(255),
                      last_name varchar(255) not null,
                      email1 varchar(255),
                      email2 varchar(255),
                      email3 varchar(255),
                      phone1 varchar(15),
                      phone2 varchar(15),
                      phone3 varchar(15),
                      pancard varchar(32),
                      passport_no varchar(32),
                      gender varchar(50),
                      house_no varchar(255),
                      street varchar(255),
                      pincode varchar(255),
                      landmark varchar(255),
                      city varchar(255),
                      state varchar(255),
                      country varchar(255),
                      full_address varchar(255),
                      date_of_birth date,
                      place_of_birth varchar(255),
                      "attributes" jsonb,
                      roles varchar[],
                      scopes varchar[],
                      status varchar(255),
                      created_at timestamp not null,
                      updated_at timestamp,
                      created_by varchar(255) not null,
                      updated_by varchar(255)
);
create index idx_user_code on "user" (code);
create index idx_user_email1 on "user" (email1);
create index idx_user_pancard on "user" (pancard);

create table address (
                      id serial primary key,
                      house_no varchar(255) not null,
                      street varchar(255),
                      pincode varchar(255),
                      landmark varchar(255),
                      city varchar(255),
                      state varchar(255),
                      country varchar(255) not null,
                      full_address varchar(255),
                      attributes jsonb,
                      created_at timestamp not null,
                      updated_at timestamp,
                      created_by varchar(255) not null,
                      updated_by varchar(255),
                      user_id integer,
                      constraint fk_user_id foreign key(user_id) references "user"(id)
);
create index idx_address_user on address (id, user_id);
