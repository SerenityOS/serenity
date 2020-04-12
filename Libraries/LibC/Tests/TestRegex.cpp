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

#include <AK/StringBuilder.h>
#include <AK/TestSuite.h>
#include <LibC/regex.h>
#include <stdio.h>

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
    EXPECT_EQ(regexec(&regex, "hello1hello1", 0, NULL, 0), REG_NOERR);
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
    EXPECT_EQ(regexec(&regex, "aaaaaabbbbb", 0, NULL, 0), REG_NOERR);
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
    EXPECT_EQ(regexec(&regex, "ddddd", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "dd", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "dad", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "dada", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "adadaa", 0, NULL, 0), REG_NOERR);

    regfree(&regex);
}

TEST_CASE(simple_questionmark_matchall)
{
    String pattern = "da?d";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "a", num_matches, matches, REG_MATCHALL), REG_NOMATCH);
    EXPECT_EQ(matches[0].match_count, 0u);
    EXPECT_EQ(regexec(&regex, "daa", num_matches, matches, REG_MATCHALL), REG_NOMATCH);
    EXPECT_EQ(matches[0].match_count, 0u);

    EXPECT_EQ(regexec(&regex, "ddddd", num_matches, matches, REG_MATCHALL), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 2u);

    //    for (int i = 0; i < num_matches; ++i) {
    //        printf("Matches[%i].rm_so: %li, .rm_eo: %li\n", i, matches[i].rm_so, matches[i].rm_eo);
    //    }

    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 2u);
    EXPECT_EQ(matches[1].rm_so, 2u);
    EXPECT_EQ(matches[1].rm_eo, 4u);

    EXPECT_EQ(regexec(&regex, "dd", num_matches, matches, REG_MATCHALL), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(regexec(&regex, "dad", num_matches, matches, REG_MATCHALL), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(regexec(&regex, "dada", num_matches, matches, REG_MATCHALL), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(regexec(&regex, "adadaa", num_matches, matches, REG_MATCHALL), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);

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

TEST_CASE(parens)
{
    String pattern = "test(hello)test";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    EXPECT_EQ(regexec(&regex, "testhellotest", num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);

    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 13u);
    EXPECT_EQ(matches[1].rm_so, 4u);
    EXPECT_EQ(matches[1].rm_eo, 9u);

    regfree(&regex);
}

TEST_CASE(parser_error_parens)
{
    String pattern = "test()test";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_BADPAT);
    EXPECT_EQ(regexec(&regex, "testhellotest", num_matches, matches, 0), REG_BADPAT);
    EXPECT_EQ(matches[0].match_count, 0u);

    regfree(&regex);
}

TEST_CASE(parser_error_special_characters_used_at_wrong_place)
{
    String pattern;
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];

    Vector<char, 4> chars = { '*', '+', '?', '}' };
    StringBuilder b;

    for (auto& ch : chars) {
        // First in ere
        b.clear();
        b.append(ch);
        pattern = b.build();
        EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_BADPAT);
        EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_BADPAT);

        // After vertical line
        b.clear();
        b.append("a|");
        b.append(ch);
        pattern = b.build();
        EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_BADPAT);
        EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_BADPAT);

        // After circumflex
        b.clear();
        b.append("^");
        b.append(ch);
        pattern = b.build();
        EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_BADPAT);
        EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_BADPAT);

        // After dollar
        b.clear();
        b.append("$");
        b.append(ch);
        pattern = b.build();
        EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_BADPAT);
        EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_BADPAT);

        // After left parens
        b.clear();
        b.append("(");
        b.append(ch);
        b.append(")");
        pattern = b.build();
        EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_BADPAT);
        EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_BADPAT);
    }

    regfree(&regex);
}

TEST_CASE(parser_error_vertical_line_used_at_wrong_place)
{
    String pattern;
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];

    // First in ere
    pattern = "|asdf";
    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_BADPAT);
    EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_BADPAT);

    // Last in ere
    pattern = "asdf|";
    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_BADPAT);
    EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_BADPAT);

    // After left parens
    pattern = "(|asdf)";
    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_BADPAT);
    EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_BADPAT);

    // Proceed right parens
    pattern = "(asdf)|";
    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_BADPAT);
    EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_BADPAT);

    regfree(&regex);
}

TEST_CASE(parens_qualifier_questionmark)
{
    String pattern = "test(hello)?test";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];
    const char* match_str;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    match_str = "testtest";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 8u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testtest");

    match_str = "testhellotest";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 13u);
    EXPECT_EQ(matches[1].rm_so, 4u);
    EXPECT_EQ(matches[1].rm_eo, 9u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testhellotest");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    regfree(&regex);
}

TEST_CASE(parens_qualifier_asterisk)
{
    String pattern = "test(hello)*test";
    regex_t regex;
    static constexpr int num_matches { 6 };
    regmatch_t matches[num_matches];
    const char* match_str;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    match_str = "testtest";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 8u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testtest");

    match_str = "testhellohellotest";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 18u);
    EXPECT_EQ(matches[1].rm_so, 9u);
    EXPECT_EQ(matches[1].rm_eo, 14u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testhellohellotest");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "testhellohellotest, testhellotest";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, REG_MATCHALL), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 2u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 18u);
    EXPECT_EQ(matches[1].rm_so, 9u);
    EXPECT_EQ(matches[1].rm_eo, 14u);
    EXPECT_EQ(matches[2].rm_so, 20u);
    EXPECT_EQ(matches[2].rm_eo, 33u);
    EXPECT_EQ(matches[3].rm_so, 24u);
    EXPECT_EQ(matches[3].rm_eo, 29u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testhellohellotest");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");
    EXPECT_EQ(StringView(&match_str[matches[2].rm_so], matches[2].rm_eo - matches[2].rm_so), "testhellotest");
    EXPECT_EQ(StringView(&match_str[matches[3].rm_so], matches[3].rm_eo - matches[3].rm_so), "hello");

    regfree(&regex);
}

TEST_CASE(parens_qualifier_asterisk_2)
{
    String pattern = "test(.*)test";
    regex_t regex;
    static constexpr int num_matches { 6 };
    regmatch_t matches[num_matches];
    const char* match_str;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    match_str = "testasdftest";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 12u);
    EXPECT_EQ(matches[1].rm_so, 4u);
    EXPECT_EQ(matches[1].rm_eo, 8u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testasdftest");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "asdf");

    match_str = "testasdfasdftest";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 16u);
    EXPECT_EQ(matches[1].rm_so, 4u);
    EXPECT_EQ(matches[1].rm_eo, 12u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testasdfasdftest");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "asdfasdf");

    match_str = "testaaaatest, testbbbtest, testtest";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, REG_MATCHALL), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 35u);
    EXPECT_EQ(matches[1].rm_so, 4u);
    EXPECT_EQ(matches[1].rm_eo, 31u);

    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testaaaatest, testbbbtest, testtest");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "aaaatest, testbbbtest, test");

    regfree(&regex);
}

TEST_CASE(mulit_parens_qualifier_too_less_result_values)
{
    String pattern = "test(a)?(b)?(c)?test";
    regex_t regex;
    static constexpr int num_matches { 4 };
    regmatch_t matches[num_matches];
    const char* match_str;

    matches[3] = { -2, -2, 100 };

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    match_str = "testabtest";
    EXPECT_EQ(regexec(&regex, match_str, num_matches - 1, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 10u);
    EXPECT_EQ(matches[1].rm_so, 4u);
    EXPECT_EQ(matches[1].rm_eo, 5u);
    EXPECT_EQ(matches[2].rm_so, 5u);
    EXPECT_EQ(matches[2].rm_eo, 6u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testabtest");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "a");
    EXPECT_EQ(StringView(&match_str[matches[2].rm_so], matches[2].rm_eo - matches[2].rm_so), "b");
    EXPECT_EQ(matches[3].rm_so, -2);
    EXPECT_EQ(matches[3].rm_eo, -2);
    EXPECT_EQ(matches[3].match_count, 100u);

    match_str = "testabctest";
    EXPECT_EQ(regexec(&regex, match_str, num_matches - 1, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 11u);
    EXPECT_EQ(matches[1].rm_so, 4u);
    EXPECT_EQ(matches[1].rm_eo, 5u);
    EXPECT_EQ(matches[2].rm_so, 5u);
    EXPECT_EQ(matches[2].rm_eo, 6u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testabctest");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "a");
    EXPECT_EQ(StringView(&match_str[matches[2].rm_so], matches[2].rm_eo - matches[2].rm_so), "b");
    EXPECT_EQ(matches[3].rm_so, -2);
    EXPECT_EQ(matches[3].rm_eo, -2);
    EXPECT_EQ(matches[3].match_count, 100u);

    match_str = "testabctest, testabctest";

    EXPECT_EQ(regexec(&regex, match_str, num_matches - 1, matches, REG_MATCHALL), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 2u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 11u);
    EXPECT_EQ(matches[1].rm_so, 4u);
    EXPECT_EQ(matches[1].rm_eo, 5u);
    EXPECT_EQ(matches[2].rm_so, 5u);
    EXPECT_EQ(matches[2].rm_eo, 6u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testabctest");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "a");
    EXPECT_EQ(StringView(&match_str[matches[2].rm_so], matches[2].rm_eo - matches[2].rm_so), "b");
    EXPECT_EQ(matches[3].rm_so, -2);
    EXPECT_EQ(matches[3].rm_eo, -2);
    EXPECT_EQ(matches[3].match_count, 100u);

    regfree(&regex);
}

TEST_CASE(multi_parens_qualifier_questionmark)
{
    String pattern = "test(a)?(b)?(c)?test";
    regex_t regex;
    static constexpr int num_matches { 8 };
    regmatch_t matches[num_matches];
    const char* match_str;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    match_str = "testtest";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 8u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testtest");

    match_str = "testabctest";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 11u);
    EXPECT_EQ(matches[1].rm_so, 4u);
    EXPECT_EQ(matches[1].rm_eo, 5u);
    EXPECT_EQ(matches[2].rm_so, 5u);
    EXPECT_EQ(matches[2].rm_eo, 6u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testabctest");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "a");
    EXPECT_EQ(StringView(&match_str[matches[2].rm_so], matches[2].rm_eo - matches[2].rm_so), "b");

    match_str = "testabctest, testactest";

    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, REG_MATCHALL), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 2u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 11u);
    EXPECT_EQ(matches[1].rm_so, 4u);
    EXPECT_EQ(matches[1].rm_eo, 5u);
    EXPECT_EQ(matches[2].rm_so, 5u);
    EXPECT_EQ(matches[2].rm_eo, 6u);
    EXPECT_EQ(matches[3].rm_so, 6u);
    EXPECT_EQ(matches[3].rm_eo, 7u);

    EXPECT_EQ(matches[4].rm_so, 13u);
    EXPECT_EQ(matches[4].rm_eo, 23u);
    EXPECT_EQ(matches[5].rm_so, 17u);
    EXPECT_EQ(matches[5].rm_eo, 18u);
    EXPECT_EQ(matches[6].rm_so, -1);
    EXPECT_EQ(matches[6].rm_eo, -1);
    EXPECT_EQ(matches[7].rm_so, 18u);
    EXPECT_EQ(matches[7].rm_eo, 19u);

    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testabctest");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "a");
    EXPECT_EQ(StringView(&match_str[matches[2].rm_so], matches[2].rm_eo - matches[2].rm_so), "b");
    EXPECT_EQ(StringView(&match_str[matches[3].rm_so], matches[3].rm_eo - matches[3].rm_so), "c");
    EXPECT_EQ(StringView(&match_str[matches[4].rm_so], matches[4].rm_eo - matches[4].rm_so), "testactest");
    EXPECT_EQ(StringView(&match_str[matches[5].rm_so], matches[5].rm_eo - matches[5].rm_so), "a");
    EXPECT_EQ(StringView(&match_str[matches[6].rm_so], matches[6].rm_eo - matches[6].rm_so), "");
    EXPECT_EQ(StringView(&match_str[matches[7].rm_so], matches[7].rm_eo - matches[7].rm_so), "c");

    //    for (int i = 0; i < num_matches; ++i) {
    //        printf("Matches[%i].rm_so: %li, .rm_eo: %li\n", i, matches[i].rm_so, matches[i].rm_eo);
    //    }

    regfree(&regex);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS))
BENCHMARK_CASE(parens_qualifier_asterisk_2_benchmark)
{
    String pattern = "test(.*)test";
    regex_t regex;
    static constexpr int num_matches { 6 };
    regmatch_t matches[num_matches];
    const char* match_str;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    match_str = "testaaaatest, testbbbtest, testtest";
    for (size_t i = 0; i < 10000; ++i) {
        EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, REG_MATCHALL), REG_NOERR);
    }

    regfree(&regex);
}
#endif

TEST_CASE(simple_alternative)
{
    String pattern = "test|hello|friends";
    regex_t regex;
    static constexpr int num_matches { 1 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 4u);

    EXPECT_EQ(regexec(&regex, "hello", num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 5u);

    EXPECT_EQ(regexec(&regex, "friends", num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 7u);

    regfree(&regex);
}

TEST_CASE(alternative_match_groups)
{
    String pattern = "test(a)?(b)?|hello ?(dear|my)? friends";
    regex_t regex;
    static constexpr int num_matches { 8 };
    regmatch_t matches[num_matches];
    const char* match_str;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    match_str = "test";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0);
    EXPECT_EQ(matches[0].rm_eo, 4);
    EXPECT_EQ(matches[1].rm_so, -1);
    EXPECT_EQ(matches[1].rm_eo, -1);
    EXPECT_EQ(matches[2].rm_so, -1);
    EXPECT_EQ(matches[2].rm_eo, -1);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "test");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "");
    EXPECT_EQ(StringView(&match_str[matches[2].rm_so], matches[2].rm_eo - matches[2].rm_so), "");

    match_str = "testa";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 5u);
    EXPECT_EQ(matches[1].rm_so, 4u);
    EXPECT_EQ(matches[1].rm_eo, 5u);
    EXPECT_EQ(matches[2].rm_so, -1);
    EXPECT_EQ(matches[2].rm_eo, -1);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testa");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "a");
    EXPECT_EQ(StringView(&match_str[matches[2].rm_so], matches[2].rm_eo - matches[2].rm_so), "");

    match_str = "testb";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 5u);
    EXPECT_EQ(matches[1].rm_so, -1);
    EXPECT_EQ(matches[1].rm_eo, -1);
    EXPECT_EQ(matches[2].rm_so, 4u);
    EXPECT_EQ(matches[2].rm_eo, 5u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "testb");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "");
    EXPECT_EQ(StringView(&match_str[matches[2].rm_so], matches[2].rm_eo - matches[2].rm_so), "b");

    match_str = "hello friends";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 13u);
    EXPECT_EQ(matches[1].rm_so, -1);
    EXPECT_EQ(matches[1].rm_eo, -1);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hello friends");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "");

    match_str = "hello dear friends";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 18u);
    EXPECT_EQ(matches[1].rm_so, -1);
    EXPECT_EQ(matches[1].rm_eo, -1);
    EXPECT_EQ(matches[2].rm_so, -1);
    EXPECT_EQ(matches[2].rm_eo, -1);
    EXPECT_EQ(matches[3].rm_so, 6);
    EXPECT_EQ(matches[3].rm_eo, 10);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hello dear friends");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "");
    EXPECT_EQ(StringView(&match_str[matches[2].rm_so], matches[2].rm_eo - matches[2].rm_so), "");
    EXPECT_EQ(StringView(&match_str[matches[3].rm_so], matches[3].rm_eo - matches[3].rm_so), "dear");

    match_str = "hello my friends";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 16u);
    EXPECT_EQ(matches[1].rm_so, -1);
    EXPECT_EQ(matches[1].rm_eo, -1);
    EXPECT_EQ(matches[2].rm_so, -1);
    EXPECT_EQ(matches[2].rm_eo, -1);
    EXPECT_EQ(matches[3].rm_so, 6);
    EXPECT_EQ(matches[3].rm_eo, 8);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hello my friends");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "");
    EXPECT_EQ(StringView(&match_str[matches[2].rm_so], matches[2].rm_eo - matches[2].rm_so), "");
    EXPECT_EQ(StringView(&match_str[matches[3].rm_so], matches[3].rm_eo - matches[3].rm_so), "my");

    match_str = "testabc";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOMATCH);
    EXPECT_EQ(matches[0].match_count, 0u);
    EXPECT_EQ(matches[0].rm_so, -1);
    EXPECT_EQ(matches[0].rm_eo, -1);

    match_str = "hello test friends";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOMATCH);
    EXPECT_EQ(matches[0].match_count, 0u);
    EXPECT_EQ(matches[0].rm_so, -1);
    EXPECT_EQ(matches[0].rm_eo, -1);

    regfree(&regex);
}

TEST_CASE(parens_qualifier_exact)
{
    String pattern = "(hello){3}";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];
    const char* match_str;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    match_str = "hello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOMATCH);
    EXPECT_EQ(matches[0].match_count, 0u);

    match_str = "hellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 15u);
    EXPECT_EQ(matches[1].rm_so, 10u);
    EXPECT_EQ(matches[1].rm_eo, 15u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "hellohellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 15u);
    EXPECT_EQ(matches[1].rm_so, 10u);
    EXPECT_EQ(matches[1].rm_eo, 15u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "test hellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 5u);
    EXPECT_EQ(matches[0].rm_eo, 20u);
    EXPECT_EQ(matches[1].rm_so, 15u);
    EXPECT_EQ(matches[1].rm_eo, 20u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    //    for (int i = 0; i < num_matches; ++i)
    //        printf("Matches[%i].rm_so: %li, .rm_eo: %li\n", i, matches[i].rm_so, matches[i].rm_eo);

    regfree(&regex);
}

TEST_CASE(parens_qualifier_minimum)
{
    String pattern = "(hello){3,}";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];
    const char* match_str;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    match_str = "hello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOMATCH);
    EXPECT_EQ(matches[0].match_count, 0u);

    match_str = "hellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 15u);
    EXPECT_EQ(matches[1].rm_so, 10u);
    EXPECT_EQ(matches[1].rm_eo, 15u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "hellohellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 20u);
    EXPECT_EQ(matches[1].rm_so, 15u);
    EXPECT_EQ(matches[1].rm_eo, 20u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "test hellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 5u);
    EXPECT_EQ(matches[0].rm_eo, 20u);
    EXPECT_EQ(matches[1].rm_so, 15u);
    EXPECT_EQ(matches[1].rm_eo, 20u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "test hellohellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 5u);
    EXPECT_EQ(matches[0].rm_eo, 25u);
    EXPECT_EQ(matches[1].rm_so, 20u);
    EXPECT_EQ(matches[1].rm_eo, 25u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    //    for (int i = 0; i < num_matches; ++i)
    //        printf("Matches[%i].rm_so: %li, .rm_eo: %li\n", i, matches[i].rm_so, matches[i].rm_eo);

    regfree(&regex);
}

TEST_CASE(parens_qualifier_maximum)
{
    String pattern = "(hello){2,3}";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];
    const char* match_str;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    match_str = "hello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOMATCH);
    EXPECT_EQ(matches[0].match_count, 0u);

    match_str = "hellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 15u);
    EXPECT_EQ(matches[1].rm_so, 10u);
    EXPECT_EQ(matches[1].rm_eo, 15u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "hellohellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 15u);
    EXPECT_EQ(matches[1].rm_so, 10u);
    EXPECT_EQ(matches[1].rm_eo, 15u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "test hellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 5u);
    EXPECT_EQ(matches[0].rm_eo, 20u);
    EXPECT_EQ(matches[1].rm_so, 15u);
    EXPECT_EQ(matches[1].rm_eo, 20u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "test hellohellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 5u);
    EXPECT_EQ(matches[0].rm_eo, 20u);
    EXPECT_EQ(matches[1].rm_so, 15u);
    EXPECT_EQ(matches[1].rm_eo, 20u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    //    for (int i = 0; i < num_matches; ++i)
    //        printf("Matches[%i].rm_so: %li, .rm_eo: %li\n", i, matches[i].rm_so, matches[i].rm_eo);

    regfree(&regex);
}

TEST_MAIN(Regex)
