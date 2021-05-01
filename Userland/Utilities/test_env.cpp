/*
 * Copyright (c) 2018-2020, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void assert_env(const char* name, const char* value)
{
    char* result = getenv(name);
    if (!result) {
        perror("getenv");
        printf("(When reading value for '%s'; we expected '%s'.)\n", name, value);
        VERIFY(false);
    }
    if (strcmp(result, value) != 0) {
        printf("Expected '%s', got '%s' instead.\n", value, result);
        VERIFY(false);
    }
}

static void test_getenv_preexisting()
{
    assert_env("HOME", "/home/anon");
}

static void test_puttenv()
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

static void test_settenv()
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

static void test_settenv_overwrite_empty()
{
    int rc = setenv("EMPTYTEST", "Forcefully overwrite non-existing envvar", 1);
    if (rc) {
        perror("setenv");
        VERIFY(false);
    }
    assert_env("EMPTYTEST", "Forcefully overwrite non-existing envvar");
}

int main(int, char**)
{
#define RUNTEST(x)                      \
    {                                   \
        printf("Running " #x " ...\n"); \
        x();                            \
        printf("Success!\n");           \
    }
    RUNTEST(test_getenv_preexisting);
    RUNTEST(test_puttenv);
    RUNTEST(test_settenv);
    RUNTEST(test_settenv_overwrite_empty);
    printf("PASS\n");

    return 0;
}
