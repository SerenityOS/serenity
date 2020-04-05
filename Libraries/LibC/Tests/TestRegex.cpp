/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include <AK/TestSuite.h>
#include <regex.h>
#include <stdio.h>

#define REG_NOERR false

TEST_CASE(catch_all)
{
    String pattern = "^.*$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "Hello World", 0, NULL, 0), REG_NOERR);

    regfree(&regex);
}

TEST_CASE(simple_start)
{
    String pattern = "^hello friends";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "Hello!", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "hello friends", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "Well, hello friends", 0, NULL, 0), REG_NOMATCH);

    regfree(&regex);
}

TEST_CASE(simple_end)
{
    String pattern = ".*hello\\.\\.\\. there$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "Hallo", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "I said fyhello... there", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "ahello... therea", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "hello.. there", 0, NULL, 0), REG_NOMATCH);

    regfree(&regex);
}

TEST_CASE(simple_period)
{
    String pattern = "hello.";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "Hello1", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "hello1", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "hello2", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "hello?", 0, NULL, 0), REG_NOERR);

    regfree(&regex);
}

TEST_CASE(simple_period_end)
{
    String pattern = "hello.$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "Hello1", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "hello1hello1", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "hello2hell", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "hello?", 0, NULL, 0), REG_NOERR);

    regfree(&regex);
}

TEST_CASE(simple_escaped)
{
    String pattern = "hello\\.";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "hello", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "hello.", 0, NULL, 0), REG_NOERR);

    regfree(&regex);
}

TEST_CASE(simple_period2_end)
{
    String pattern = ".*hi... there$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "Hello there", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "I said fyhi... there", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "....hi... ", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "I said fyhihii there", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "I said fyhihi there", 0, NULL, 0), REG_NOMATCH);

    regfree(&regex);
}

TEST_CASE(simple_plus)
{
    String pattern = "a+";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "b", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "a", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "aaaaaabbbbb", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "aaaaaaaaaaa", 0, NULL, 0), REG_NOERR);

    regfree(&regex);
}

TEST_CASE(simple_questionmark)
{
    String pattern = "da?d";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "a", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "daa", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "ddddd", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "dd", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "dad", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "dada", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "adadaa", 0, NULL, 0), REG_NOMATCH);

    regfree(&regex);
}

TEST_CASE(escaped_char_questionmark)
{
    String pattern = "This\\.?And\\.?That";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "ThisAndThat", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "This.And.That", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "This And That", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "This..And..That", 0, NULL, 0), REG_NOMATCH);

    regfree(&regex);
}

TEST_CASE(complex1)
{
    String pattern = "^[A-Z0-9._%+-]{1,64}@(?:[A-Z0-9-]{1,63}\\.){1,125}[A-Z]{2,63}$";
    regex_t regex;

    // Braket parsing not supported yet
    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_BADPAT);
}

TEST_MAIN(Regex)
