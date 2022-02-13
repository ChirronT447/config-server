package com.config.configserver.service;

/**
 * See src/main/resources/db/changelog/changelog.sql for table structure
 */
public class SqlStatements {

    public static final String APP_SQL = """
        SELECT to_jsonb(array_agg(res))
        FROM (
            SELECT * FROM PROPERTIES
            WHERE APPLICATION = ?
        ) res
    """;

    public static final String APP_PROFILE_SQL = """
        SELECT to_jsonb(array_agg(res))
        FROM (
           SELECT * FROM PROPERTIES
           WHERE APPLICATION = ?
               AND PROFILE = ?
        ) res
    """;

    public static final String APP_PROFILE_KEY_SQL = """
        SELECT to_jsonb(array_agg(res))
        FROM (
           SELECT * FROM PROPERTIES
           WHERE APPLICATION = ?
               AND PROFILE = ?
               AND PROP_KEY = ?
        ) res
    """;

    public static final String SINGLE_INSERT = """
        INSERT INTO public.properties(application, profile, label, prop_key, value)
        VALUES (
            ?, ?, ?, ?, ?
        );
    """;

//    public static final String BULK_INSERT = """
//        COPY PROPERTIES FROM STDIN
//        VACUUM ANALYZE
//    """;

    public static final String UPDATE = """
        UPDATE public.properties
        SET created_on=?, application=?, profile=?, label=?, prop_key=?, value=?
        WHERE <condition>;
    """;
}
