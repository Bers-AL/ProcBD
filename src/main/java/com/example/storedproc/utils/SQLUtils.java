package com.example.storedproc.utils;

public class SQLUtils {

    public static String generateCreateProcedureSQL(String name, String parameters, String body) {
        return "CREATE OR REPLACE PROCEDURE " + name + " (" + parameters + ") LANGUAGE plpgsql AS $$ BEGIN " + body + " END; $$";
    }
}
