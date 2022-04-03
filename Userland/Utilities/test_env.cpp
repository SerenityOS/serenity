/*
 * Copyright (c) 2018-2020, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void assert_env(char const* name, char const* value)
{
    char* result = getenv(name);
    if (!result) {
        perror("getenv");
        outln("(When reading value for '{}'; we expected '{}'.)", name, value);
        VERIFY(false);
    }
    if (strcmp(result, value) != 0) {
        outln("Expected '{}', got '{}' instead.", value, result);
        VERIFY(false);
    }
}

static void test_getenv_preexisting()
{
    assert_env("HOME", "/home/anon");
}

static void test_putenv()
{
    char* to_put = strdup("PUTENVTEST=HELLOPUTENV");
    int rc = putenv(to_put);
    if (rc) {
        perror("putenv");
        VERIFY(false);
    }
    assert_env("PUTENVTEST", "HELLOPUTENV");
    // Do not free `to_put`!
}

static void test_setenv()
{
    int rc = setenv("SETENVTEST", "HELLO SETENV!", 0);
    if (rc) {
        perror("setenv");
        VERIFY(false);
    }
    // This used to trigger a very silly bug! :)
    assert_env("SETENVTEST", "HELLO SETENV!");

    rc = setenv("SETENVTEST", "How are you today?", 0);
    if (rc) {
        perror("setenv");
        VERIFY(false);
    }
    assert_env("SETENVTEST", "HELLO SETENV!");

    rc = setenv("SETENVTEST", "Goodbye, friend!", 1);
    if (rc) {
        perror("setenv");
        VERIFY(false);
    }
    assert_env("SETENVTEST", "Goodbye, friend!");
}

static void test_setenv_overwrite_empty()
{
    int rc = setenv("EMPTYTEST", "Forcefully overwrite non-existing envvar", 1);
    if (rc) {
        perror("setenv");
        VERIFY(false);
    }
    assert_env("EMPTYTEST", "Forcefully overwrite non-existing envvar");
}

ErrorOr<int> serenity_main(Main::Arguments)
{
#define RUNTEST(x)                   \
    {                                \
        outln("Running " #x " ..."); \
        x();                         \
        outln("Success!");           \
    }
    RUNTEST(test_getenv_preexisting);
    RUNTEST(test_putenv);
    RUNTEST(test_setenv);
    RUNTEST(test_setenv_overwrite_empty);
    outln("PASS");

    return 0;
}
