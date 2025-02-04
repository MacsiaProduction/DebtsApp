CREATE TABLE "kurultais"
(
    "id"       uuid PRIMARY KEY,
    "name"     text      NOT NULL
);

CREATE TABLE "kurultai_visitors"
(
    "id"          uuid PRIMARY KEY,
    "kurultai_id" uuid NOT NULL,
    "person_name" text
);

CREATE TABLE "kurultai_visitor_sessions"
(
    "id"                  uuid PRIMARY KEY,
    "kurultai_visitor_id" uuid      NOT NULL,
    "enter_time"          timestamp NOT NULL,
    "exit_time"           timestamp NOT NULL
);

CREATE TABLE "person_roles"
(
    "kurultai_id" uuid    NOT NULL,
    "person_name" text    NOT NULL,
    "is_owner"    boolean NOT NULL DEFAULT FALSE,
    PRIMARY KEY (kurultai_id, person_name)
);

ALTER TABLE "kurultai_visitors"
    ADD FOREIGN KEY ("kurultai_id") REFERENCES "kurultais" ("id");

ALTER TABLE "kurultai_visitor_sessions"
    ADD FOREIGN KEY ("kurultai_visitor_id") REFERENCES "kurultai_visitors" (id);

ALTER TABLE "kurultai_visitors"
    ADD FOREIGN KEY ("kurultai_id", "person_name") REFERENCES "person_roles" ("kurultai_id", "person_name");

ALTER TABLE "person_roles"
    ADD FOREIGN KEY ("kurultai_id") REFERENCES "kurultais" ("id");


CREATE UNIQUE INDEX kurultai_visitors_unique_idx ON kurultai_visitors (kurultai_id, person_name) WHERE person_name is not null;
CREATE UNIQUE INDEX person_roles_unique_idx ON person_roles (kurultai_id, person_name);
CREATE UNIQUE INDEX owner_person_roles_unique_idx ON person_roles (kurultai_id) WHERE is_owner;

CREATE INDEX ON kurultai_visitors (kurultai_id);
CREATE INDEX ON kurultai_visitors (kurultai_id, person_name);
CREATE INDEX ON kurultai_visitor_sessions (kurultai_visitor_id);
CREATE INDEX ON person_roles (kurultai_id, person_name);




WITH session_durations AS (
    SELECT
        kv.kurultai_id,
        kv.person_name,
        kvs.enter_time,
        kvs.exit_time,
        kv.id AS visitor_id,
        EXTRACT(EPOCH FROM (kvs.exit_time - kvs.enter_time)) AS session_duration
    FROM
        kurultai_visitors kv
            JOIN
        kurultai_visitor_sessions kvs ON kv.id = kvs.kurultai_visitor_id
),

-- Оставляем только сессии, которые длились более 30 секунд
     valid_sessions AS (
         SELECT DISTINCT
             kurultai_id,
             visitor_id,
             CASE
                 WHEN person_name IS NULL THEN 'ANONYMOUS_' || visitor_id::text || '_' || EXTRACT(EPOCH FROM enter_time)::text
                 ELSE person_name
                 END AS unique_visitor,
             enter_time,
             exit_time
         FROM
             session_durations
         WHERE
             session_duration > 30
     ),

-- Определяем количество уникальных посетителей для каждого момента времени
     overlapping_sessions AS (
         SELECT
             vs1.kurultai_id,
             vs1.unique_visitor,
             COUNT(DISTINCT vs2.unique_visitor) AS overlapping_count
         FROM
             valid_sessions vs1
                 JOIN
             valid_sessions vs2 ON vs1.kurultai_id = vs2.kurultai_id
                 AND vs1.unique_visitor != vs2.unique_visitor
                 AND vs1.enter_time <= vs2.exit_time
                 AND vs2.enter_time <= vs1.exit_time
         GROUP BY
             vs1.kurultai_id, vs1.unique_visitor
         HAVING
             COUNT(DISTINCT vs2.unique_visitor) >= 9  -- 9 других участников + сам = 10 одновременных
     ),

-- Выбираем все курултаи, где было 10 или более одновременных участников
     kurultai_with_min_participants AS (
         SELECT DISTINCT
             kurultai_id
         FROM
             overlapping_sessions
     ),

-- Считаем количество курултаев, организованных каждым владельцем
     owner_kurultai_count AS (
         SELECT
             pr.person_name,
             COUNT(*) AS count
         FROM
             person_roles pr
                 JOIN
             kurultai_with_min_participants kmp ON pr.kurultai_id = kmp.kurultai_id
         WHERE
             pr.is_owner = true
         GROUP BY
             pr.person_name
     )

-- Финальный вывод с сортировкой по убыванию количества и лексикографическому порядку person_name
SELECT
    person_name,
    count
FROM
    owner_kurultai_count
ORDER BY
    count DESC,
    person_name ASC
LIMIT 10;

