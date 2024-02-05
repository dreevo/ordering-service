CREATE TABLE orders
(
    id                 BIGSERIAL PRIMARY KEY NOT NULL,
    food_ref          varchar(255)          NOT NULL,
    food_description varchar(255),
    food_price         float8,
    quantity           int                   NOT NULL,
    status             varchar(255)          NOT NULL,
    created_date       timestamp             NOT NULL,
    last_modified_date timestamp             NOT NULL,
    version            integer               NOT NULL
);