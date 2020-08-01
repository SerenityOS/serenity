/*
 * Copyright (c) 2018-2020, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        ASSERT(false);
    }
    if (strcmp(result, value) != 0) {
        printf("Expected '%s', got '%s' instead.\n", value, result);
        ASSERT(false);
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
        ASSERT(false);
    }
    assert_env("PUTENVTEST", "HELLOPUTENV");
    // Do not free `to_put`!
}

static void test_settenv()
{
    int rc = setenv("SETENVTEST", "HELLO SETENV!", 0);
    if (rc) {
        perror("setenv");
        ASSERT(false);
    }
    // This used to trigger a very silly bug! :)
    assert_env("SETENVTEST", "HELLO SETENV!");

    rc = setenv("SETENVTEST", "How are you today?", 0);
    if (rc) {
        perror("setenv");
        ASSERT(false);
    }
    assert_env("SETENVTEST", "HELLO SETENV!");

    rc = setenv("SETENVTEST", "Goodbye, friend!", 1);
    if (rc) {
        perror("setenv");
        ASSERT(false);
    }
    assert_env("SETENVTEST", "Goodbye, friend!");
}

static void test_settenv_overwrite_empty()
{
    int rc = setenv("EMPTYTEST", "Forcefully overwrite non-existing envvar", 1);
    if (rc) {
        perror("setenv");
        ASSERT(false);
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
