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

#define BENCHMARK_LOOP_ITERATIONS 100000
//#define DISABLE_REGEX_BENCHMARK

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
#    include <regex>
#endif

TEST_CASE(catch_all)
{
    String pattern = "^.*$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "Hello World", 0, NULL, 0), REG_NOERR);

    regfree(&regex);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(catch_all_benchmark)
{
    String pattern = "^.*$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "Hello World", 0, NULL, 0), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(catch_all_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("^.*$");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("Hello World", m, re), true);
    }
}
#endif

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

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_start_benchmark)
{
    String pattern = "^hello friends";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "Hello!", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hello friends", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "Well, hello friends", 0, NULL, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_start_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("^hello friends");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("Hello", m, re), false);
        EXPECT_EQ(std::regex_match("hello friends", m, re), true);
        EXPECT_EQ(std::regex_match("Well, hello friends", m, re), false);
    }
}
#endif

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

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_end_benchmark)
{
    String pattern = ".*hello\\.\\.\\. there$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "Hallo", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "I said fyhello... there", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "ahello... therea", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hello.. there", 0, NULL, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_end_benchmark_reference_stdcpp_regex_search)
{
    std::regex re(".*hello\\.\\.\\. there$");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_search("Hallo", m, re), false);
        EXPECT_EQ(std::regex_search("I said fyhello... there", m, re), true);
        EXPECT_EQ(std::regex_search("ahello... therea", m, re), false);
        EXPECT_EQ(std::regex_search("hello.. there", m, re), false);
    }
}
#endif

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

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_period_benchmark)
{
    String pattern = "hello.";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "Hello1", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hello1", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "hello2", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "hello?", 0, NULL, 0), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_period_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("hello.");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("Hello1", m, re), false);
        EXPECT_EQ(std::regex_match("hello1", m, re), true);
        EXPECT_EQ(std::regex_match("hello2", m, re), true);
        EXPECT_EQ(std::regex_match("hello?", m, re), true);
    }
}
#endif

TEST_CASE(simple_period_end)
{
    String pattern = "hello.$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "Hello1", 0, NULL, REG_SEARCH), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "hello1hello1", 0, NULL, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "hello2hell", 0, NULL, REG_SEARCH), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "hello?", 0, NULL, REG_SEARCH), REG_NOERR);

    regfree(&regex);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_period_end_benchmark)
{
    String pattern = "hello.$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "Hello1", 0, NULL, REG_SEARCH), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hello1hello1", 0, NULL, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "hello2hell", 0, NULL, REG_SEARCH), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hello?", 0, NULL, REG_SEARCH), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_period_end_benchmark_reference_stdcpp_regex_search)
{
    std::regex re("hello.$");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_search("Hello1", m, re), false);
        EXPECT_EQ(std::regex_search("hello1hello1", m, re), true); // this is not matching with std::regex_match
        EXPECT_EQ(std::regex_search("hello2hell", m, re), false);
        EXPECT_EQ(std::regex_search("hello?", m, re), true);
    }
}
#endif

TEST_CASE(simple_escaped)
{
    String pattern = "hello\\.";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "hello", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "hello.", 0, NULL, 0), REG_NOERR);

    regfree(&regex);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_escaped_benchmark)
{
    String pattern = "hello\\.";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "hello", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hello.", 0, NULL, 0), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_escaped_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("hello\\.");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("hello", m, re), false);
        EXPECT_EQ(std::regex_match("hello.", m, re), true);
    }
}
#endif

TEST_CASE(simple_period2_end)
{
    String pattern = ".*hi... there$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "Hello there", 0, NULL, REG_SEARCH), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "I said fyhi... there", 0, NULL, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "....hi... ", 0, NULL, REG_SEARCH), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "I said fyhihii there", 0, NULL, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "I said fyhihi there", 0, NULL, REG_SEARCH), REG_NOMATCH);

    regfree(&regex);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_period2_end_benchmark)
{
    String pattern = ".*hi... there$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "Hello there", 0, NULL, REG_SEARCH), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "I said fyhi... there", 0, NULL, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "....hi... ", 0, NULL, REG_SEARCH), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "I said fyhihii there", 0, NULL, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "I said fyhihi there", 0, NULL, REG_SEARCH), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_period2_end_benchmark_reference_stdcpp_regex_search)
{
    std::regex re(".*hi... there$");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_search("Hello there", m, re), false);
        EXPECT_EQ(std::regex_search("I said fyhi... there", m, re), true);
        EXPECT_EQ(std::regex_search("....hi... ", m, re), false);
        EXPECT_EQ(std::regex_search("I said fyhihii there", m, re), true);
        EXPECT_EQ(std::regex_search("I said fyhihi there", m, re), false);
    }
}
#endif

TEST_CASE(simple_plus)
{
    String pattern = "a+";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "b", 0, NULL, REG_SEARCH), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "a", 0, NULL, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "aaaaaabbbbb", 0, NULL, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "aaaaaaaaaaa", 0, NULL, REG_SEARCH), REG_NOERR);

    regfree(&regex);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_plus_benchmark)
{
    String pattern = "a+";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "b", 0, NULL, REG_SEARCH), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "a", 0, NULL, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "aaaaaabbbbb", 0, NULL, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "aaaaaaaaaaa", 0, NULL, REG_SEARCH), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_plus_benchmark_reference_stdcpp_regex_search)
{
    std::regex re("a+");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_search("b", m, re), false);
        EXPECT_EQ(std::regex_search("a", m, re), true);
        EXPECT_EQ(std::regex_search("aaaaaabbbbb", m, re), true);
        EXPECT_EQ(std::regex_search("aaaaaaaaaaa", m, re), true);
    }
}
#endif

TEST_CASE(simple_questionmark)
{
    String pattern = "da?d";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "a", 0, NULL, REG_SEARCH), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "daa", 0, NULL, REG_SEARCH), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "ddddd", 0, NULL, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "dd", 0, NULL, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "dad", 0, NULL, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "dada", 0, NULL, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "adadaa", 0, NULL, REG_SEARCH), REG_NOERR);

    regfree(&regex);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_questionmark_benchmark)
{
    String pattern = "da?d";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "a", 0, NULL, REG_SEARCH), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "daa", 0, NULL, REG_SEARCH), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "ddddd", 0, NULL, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "dd", 0, NULL, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "dad", 0, NULL, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "dada", 0, NULL, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "adadaa", 0, NULL, REG_SEARCH), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_questionmark_benchmark_reference_stdcpp_regex_search)
{
    std::regex re("da?d");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_search("a", m, re), false);
        EXPECT_EQ(std::regex_search("daa", m, re), false);
        EXPECT_EQ(std::regex_search("ddddd", m, re), true);
        EXPECT_EQ(std::regex_search("dd", m, re), true);
        EXPECT_EQ(std::regex_search("dad", m, re), true);
        EXPECT_EQ(std::regex_search("dada", m, re), true);
        EXPECT_EQ(std::regex_search("adadaa", m, re), true);
    }
}
#endif

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

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_questionmark_matchall_benchmark)
{
    String pattern = "da?d";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "a", num_matches, matches, REG_MATCHALL), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "daa", num_matches, matches, REG_MATCHALL), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "ddddd", num_matches, matches, REG_MATCHALL), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "dd", num_matches, matches, REG_MATCHALL), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "dad", num_matches, matches, REG_MATCHALL), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "dada", num_matches, matches, REG_MATCHALL), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "adadaa", num_matches, matches, REG_MATCHALL), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_questionmark_matchall_benchmark_reference_stdcpp_regex_search)
{
    std::regex re("da?d");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_search("a", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_search("daa", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_search("ddddd", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("dd", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("dad", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("dada", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("adadaa", m, re, std::regex_constants::match_any), true);
    }
}
#endif

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

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(escaped_char_questionmark_benchmark)
{
    String pattern = "This\\.?And\\.?That";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "ThisAndThat", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "This.And.That", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "This And That", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "This..And..That", 0, NULL, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(escaped_char_questionmark_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("This\\.?And\\.?That");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("ThisAndThat", m, re), true);
        EXPECT_EQ(std::regex_match("This.And.That", m, re), true);
        EXPECT_EQ(std::regex_match("This And That", m, re), false);
        EXPECT_EQ(std::regex_match("This..And..That", m, re), false);
    }
}
#endif

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

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(parens_qualifier_questionmark_benchmark)
{
    String pattern = "test(hello)?test";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "testtest", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "testhellotest", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "testasfdtest", num_matches, matches, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(parens_qualifier_questionmark_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("test(hello)?test");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("testtest", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("testhellotest", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("testasfdtest", m, re, std::regex_constants::match_any), false);
    }
}
#endif

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

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(parens_qualifier_asterisk_benchmark)
{
    String pattern = "test(hello)*test";
    regex_t regex;
    static constexpr int num_matches { 6 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "testtest", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "testhellohellotest", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "testhellohellotest, testhellotest", num_matches, matches, REG_MATCHALL), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "aaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb", num_matches, matches, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(parens_qualifier_asterisk_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("test(hello)*test");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("testtest", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("testhellohellotest", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("testhellohellotest, testhellotest", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("aaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb", m, re, std::regex_constants::match_any), false);
    }
}
#endif

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

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(parens_qualifier_asterisk_2_benchmark)
{
    String pattern = "test(.*)test";
    regex_t regex;
    static constexpr int num_matches { 6 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "testasdftest", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "testasdfasdftest", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "testaaaatest, testbbbtest, testtest", num_matches, matches, REG_MATCHALL), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "aaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb", num_matches, matches, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(parens_qualifier_asterisk_2_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("test(.*)test");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("testasdftest", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("testasdfasdftest", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("testaaaatest, testbbbtest, testtest", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("aaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb", m, re, std::regex_constants::match_any), false);
    }
}
#endif

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

    regfree(&regex);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(multi_parens_qualifier_questionmark_benchmark)
{
    String pattern = "test(a)?(b)?(c)?test";
    regex_t regex;
    static constexpr int num_matches { 8 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "testtest", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "testabctest", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "testabctest, testactest", num_matches, matches, REG_MATCHALL), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "aaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb", num_matches, matches, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "whaaaaat", num_matches, matches, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(multi_parens_qualifier_questionmark_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("test(a)?(b)?(c)?test");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("testtest", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("testabctest", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("testabctest, testactest", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("aaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("test", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("whaaaaat", m, re, std::regex_constants::match_any), false);
    }
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

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_alternative_benchmark)
{
    String pattern = "test|hello|friends";
    regex_t regex;
    static constexpr int num_matches { 8 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "hello", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "friends", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "whaaaaat", num_matches, matches, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_alternative_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("test|hello|friends");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("test", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("hello", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("friends", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("whaaaaat", m, re, std::regex_constants::match_any), false);
    }
}
#endif

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

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(alternative_match_groups_benchmark)
{
    String pattern = "test(a)?(b)?|hello ?(dear|my)? friends";
    regex_t regex;
    static constexpr int num_matches { 8 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "test", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "testa", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "testb", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "hello friends", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "hello dear friends", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "hello my friends", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "testabc", num_matches, matches, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hello test friends", num_matches, matches, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(alternative_match_groups_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("test(a)?(b)?|hello ?(dear|my)? friends");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("test", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("testa", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("testb", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("hello friends", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("hello dear friends", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("hello my friends", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("testabc", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("hello test friends", m, re, std::regex_constants::match_any), false);
    }
}
#endif

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
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 15u);
    EXPECT_EQ(matches[1].rm_so, 10u);
    EXPECT_EQ(matches[1].rm_eo, 15u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "test hellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 5u);
    EXPECT_EQ(matches[0].rm_eo, 20u);
    EXPECT_EQ(matches[1].rm_so, 15u);
    EXPECT_EQ(matches[1].rm_eo, 20u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    regfree(&regex);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(parens_qualifier_exact_benchmark)
{
    String pattern = "(hello){3}";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "hello", num_matches, matches, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hellohellohello", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "hellohellohellohello", num_matches, matches, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "test hellohellohello", num_matches, matches, REG_SEARCH), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(parens_qualifier_exact_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("(hello){3}");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("hello", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("hellohellohello", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("hellohellohellohello", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("test hellohellohello", m, re, std::regex_constants::match_any), true);
    }
}
#endif

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
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 20u);
    EXPECT_EQ(matches[1].rm_so, 15u);
    EXPECT_EQ(matches[1].rm_eo, 20u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "test hellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 5u);
    EXPECT_EQ(matches[0].rm_eo, 20u);
    EXPECT_EQ(matches[1].rm_so, 15u);
    EXPECT_EQ(matches[1].rm_eo, 20u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "test hellohellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 5u);
    EXPECT_EQ(matches[0].rm_eo, 25u);
    EXPECT_EQ(matches[1].rm_so, 20u);
    EXPECT_EQ(matches[1].rm_eo, 25u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    regfree(&regex);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(parens_qualifier_minimum_benchmark)
{
    String pattern = "(hello){3,}";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "hello", num_matches, matches, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hellohellohello", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "hellohellohellohello", num_matches, matches, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "test hellohellohello", num_matches, matches, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "test hellohellohellohello", num_matches, matches, REG_SEARCH), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(parens_qualifier_minimum_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("(hello){3,}");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("hello", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("hellohellohello", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("hellohellohellohello", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("test hellohellohello", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("test hellohellohellohello", m, re, std::regex_constants::match_any), true);
    }
}
#endif

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
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 0u);
    EXPECT_EQ(matches[0].rm_eo, 15u);
    EXPECT_EQ(matches[1].rm_so, 10u);
    EXPECT_EQ(matches[1].rm_eo, 15u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "test hellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(matches[0].rm_so, 5u);
    EXPECT_EQ(matches[0].rm_eo, 20u);
    EXPECT_EQ(matches[1].rm_so, 15u);
    EXPECT_EQ(matches[1].rm_eo, 20u);
    EXPECT_EQ(StringView(&match_str[matches[0].rm_so], matches[0].rm_eo - matches[0].rm_so), "hellohellohello");
    EXPECT_EQ(StringView(&match_str[matches[1].rm_so], matches[1].rm_eo - matches[1].rm_so), "hello");

    match_str = "test hellohellohellohello";
    EXPECT_EQ(regexec(&regex, match_str, num_matches, matches, REG_SEARCH), REG_NOERR);
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

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(parens_qualifier_maximum_benchmark)
{
    String pattern = "(hello){2,3}";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "hello", num_matches, matches, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hellohellohello", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "hellohellohellohello", num_matches, matches, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "test hellohellohello", num_matches, matches, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "test hellohellohellohello", num_matches, matches, REG_SEARCH), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(parens_qualifier_maximum_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("(hello){2,3}");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("hello", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("hellohellohello", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("hellohellohellohello", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("test hellohellohello", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_search("test hellohellohellohello", m, re, std::regex_constants::match_any), true);
    }
}
#endif

TEST_CASE(char_qualifier_min_max)
{
    String pattern = "c{3,30}";
    regex_t regex;
    static constexpr int num_matches { 5 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    EXPECT_EQ(regexec(&regex, "cc", num_matches, matches, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "ccc", num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "cccccccccccccccccccccccccccccc", num_matches, matches, 0), REG_NOERR);
    EXPECT_EQ(matches[0].match_count, 1u);
    EXPECT_EQ(regexec(&regex, "ccccccccccccccccccccccccccccccc", num_matches, matches, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "ccccccccccccccccccccccccccccccc", num_matches, matches, REG_SEARCH), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "cccccccccccccccccccccccccccccccc", num_matches, matches, 0), REG_NOMATCH);

    regfree(&regex);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(char_qualifier_min_max_benchmark)
{
    String pattern = "c{3,30}";
    regex_t regex;
    static constexpr int num_matches { 6 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "cc", num_matches, matches, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "ccc", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "cccccccccccccccccccccccccccccc", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(matches[0].match_count, 1u);
        EXPECT_EQ(regexec(&regex, "ccccccccccccccccccccccccccccccc", num_matches, matches, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "ccccccccccccccccccccccccccccccc", num_matches, matches, REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "cccccccccccccccccccccccccccccccc", num_matches, matches, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(char_qualifier_min_max_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("c{3,30}");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("cc", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("ccc", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("cccccccccccccccccccccccccccccc", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("ccccccccccccccccccccccccccccccc", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_search("ccccccccccccccccccccccccccccccc", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("cccccccccccccccccccccccccccccccc", m, re, std::regex_constants::match_any), false);
    }
}
#endif

TEST_CASE(simple_bracket_chars)
{
    String pattern = "[abc]";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "a", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "b", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "c", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "d", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "e", 0, NULL, 0), REG_NOMATCH);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_bracket_chars_benchmark)
{
    String pattern = "[abc]";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "a", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "b", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "c", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "d", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "e", 0, NULL, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_bracket_chars_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("[abc]");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("a", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("b", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("c", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("d", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("e", m, re, std::regex_constants::match_any), false);
    }
}
#endif

TEST_CASE(simple_bracket_chars_inverse)
{
    String pattern = "[^abc]";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "a", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "b", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "c", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "d", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "e", 0, NULL, 0), REG_NOERR);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_bracket_chars_inverse_benchmark)
{
    String pattern = "[^abc]";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "a", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "b", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "c", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "d", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "e", 0, NULL, 0), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_bracket_chars_inverse_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("[^abc]");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("a", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("b", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("c", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("d", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("e", m, re, std::regex_constants::match_any), true);
    }
}
#endif

TEST_CASE(simple_bracket_chars_range)
{
    String pattern = "[a-d]";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "a", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "b", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "c", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "d", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "e", 0, NULL, 0), REG_NOMATCH);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_bracket_chars_range_benchmark)
{
    String pattern = "[a-d]";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "a", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "b", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "c", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "d", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "e", 0, NULL, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_bracket_chars_range_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("[a-d]");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("a", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("b", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("c", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("d", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("e", m, re, std::regex_constants::match_any), false);
    }
}
#endif

TEST_CASE(simple_bracket_chars_range_inverse)
{
    String pattern = "[^a-df-z]";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "a", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "b", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "c", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "d", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "e", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "k", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "z", 0, NULL, 0), REG_NOMATCH);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_bracket_chars_range_inverse_benchmark)
{
    String pattern = "[^a-df-z]";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "a", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "b", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "c", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "d", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "e", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "k", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "z", 0, NULL, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_bracket_chars_range_inverse_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("[^a-df-z]");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("a", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("b", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("c", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("d", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("e", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("k", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("z", m, re, std::regex_constants::match_any), false);
    }
}
#endif

TEST_CASE(bracket_character_class_uuid)
{
    String pattern = "^([[:xdigit:]]{8})-([[:xdigit:]]{4})-([[:xdigit:]]{4})-([[:xdigit:]]{4})-([[:xdigit:]]{12})$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "fb9b62a2-1579-4e3a-afba-76239ccb6583", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "fb9b62a2", 0, NULL, 0), REG_NOMATCH);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(bracket_character_class_uuid_benchmark)
{
    String pattern = "^([[:xdigit:]]{8})-([[:xdigit:]]{4})-([[:xdigit:]]{4})-([[:xdigit:]]{4})-([[:xdigit:]]{12})$";
    regex_t regex;
    static constexpr int num_matches { 6 };
    regmatch_t matches[num_matches];

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "fb9b62a2-1579-4e3a-afba-76239ccb6583", num_matches, matches, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "fb9b62a2", num_matches, matches, 0), REG_NOMATCH);
    }

    regfree(&regex);
}

BENCHMARK_CASE(bracket_character_class_uuid_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("^([[:xdigit:]]{8})-([[:xdigit:]]{4})-([[:xdigit:]]{4})-([[:xdigit:]]{4})-([[:xdigit:]]{12})$");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("fb9b62a2-1579-4e3a-afba-76239ccb6583", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("fb9b62a2", m, re, std::regex_constants::match_any), false);
    }
}
#endif

TEST_CASE(simple_bracket_character_class_inverse)
{
    String pattern = "[^[:digit:]]";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "1", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "2", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "3", 0, NULL, 0), REG_NOMATCH);
    EXPECT_EQ(regexec(&regex, "d", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "e", 0, NULL, 0), REG_NOERR);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(simple_bracket_character_class_inverse_benchmark)
{
    String pattern = "[^[:digit:]]";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "1", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "2", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "3", 0, NULL, 0), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "d", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "e", 0, NULL, 0), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(simple_bracket_character_class_inverse_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("[^[:digit:]]");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("1", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("2", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("3", m, re, std::regex_constants::match_any), false);
        EXPECT_EQ(std::regex_match("d", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("e", m, re, std::regex_constants::match_any), true);
    }
}
#endif

TEST_CASE(email_address)
{
    String pattern = "^[A-Z0-9a-z._%+-]{1,64}@[A-Za-z0-9-]{1,63}\\.{1,125}[A-Za-z]{2,63}$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "emanuel.sprung@gmail.com", 0, NULL, 0), REG_NOERR);
    EXPECT_EQ(regexec(&regex, "kling@serenityos.org", 0, NULL, 0), REG_NOERR);
}

#if not(defined(REGEX_DEBUG) || defined(REGEX_MATCH_STATUS) || defined(DISABLE_REGEX_BENCHMARK))
BENCHMARK_CASE(email_address_benchmark)
{
    String pattern = "^[A-Z0-9a-z._%+-]{1,64}@[A-Za-z0-9-]{1,63}\\.{1,125}[A-Za-z]{2,63}$";
    regex_t regex;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(regexec(&regex, "emanuel.sprung@gmail.com", 0, NULL, 0), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "kling@serenityos.org", 0, NULL, 0), REG_NOERR);
    }

    regfree(&regex);
}

BENCHMARK_CASE(email_address_benchmark_reference_stdcpp_regex_match)
{
    std::regex re("^[A-Z0-9a-z._%+-]{1,64}@[A-Za-z0-9-]{1,63}\\.{1,125}[A-Za-z]{2,63}$");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("emanuel.sprung@gmail.com", m, re, std::regex_constants::match_any), true);
        EXPECT_EQ(std::regex_match("kling@serenityos.org", m, re, std::regex_constants::match_any), true);
    }
}
#endif

TEST_MAIN(Regex)
