/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h> // import first, to prevent warning of VERIFY* redefinition

#include <LibRegex/Regex.h>
#include <stdio.h>

#ifndef REGEX_DEBUG

#    define BENCHMARK_LOOP_ITERATIONS 100000

//#    define REGEX_BENCHMARK_OUR
#    ifndef __serenity__
//#        define REGEX_BENCHMARK_OTHER
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
#        include <regex>
#    endif

#    if not(defined(REGEX_BENCHMARK_OUR) && defined(REGEX_BENCHMARK_OUR))
BENCHMARK_CASE(dummy_benchmark)
{
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(catch_all_benchmark)
{
    Regex<PosixExtended> re("^.*$");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT(re.match("Hello World", m));
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(catch_all_benchmark_reference_stdcpp)
{
    std::regex re("^.*$");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("Hello World", m, re), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_start_benchmark)
{
    Regex<PosixExtended> re("^hello friends");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("Hello!", m), false);
        EXPECT_EQ(re.match("hello friends", m), true);
        EXPECT_EQ(re.match("Well, hello friends", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_start_benchmark_reference_stdcpp)
{
    std::regex re("^hello friends");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("Hello", m, re), false);
        EXPECT_EQ(std::regex_match("hello friends", m, re), true);
        EXPECT_EQ(std::regex_match("Well, hello friends", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_end_benchmark)
{
    Regex<PosixExtended> re(".*hello\\.\\.\\. there$");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("Hallo", m), false);
        EXPECT_EQ(re.match("I said fyhello... there", m), true);
        EXPECT_EQ(re.match("ahello... therea", m), false);
        EXPECT_EQ(re.match("hello.. there", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_end_benchmark_reference_stdcpp)
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
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_period_benchmark)
{
    Regex<PosixExtended> re("hello.");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("Hello1", m), false);
        EXPECT_EQ(re.match("hello1", m), true);
        EXPECT_EQ(re.match("hello2", m), true);
        EXPECT_EQ(re.match("hello?", m), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_period_benchmark_reference_stdcpp)
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
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_period_end_benchmark)
{
    Regex<PosixExtended> re("hello.$");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.search("Hello1", m), false);
        EXPECT_EQ(re.search("hello1hello1", m), true);
        EXPECT_EQ(re.search("hello2hell", m), false);
        EXPECT_EQ(re.search("hello?", m), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_period_end_benchmark_reference_stdcpp)
{
    std::regex re("hello.$");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_search("Hello1", m, re), false);
        EXPECT_EQ(std::regex_search("hello1hello1", m, re), true);
        EXPECT_EQ(std::regex_search("hello2hell", m, re), false);
        EXPECT_EQ(std::regex_search("hello?", m, re), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_escaped_benchmark)
{
    Regex<PosixExtended> re("hello\\.");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("hello", m), false);
        EXPECT_EQ(re.match("hello.", m), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_escaped_benchmark_reference_stdcpp)
{
    std::regex re("hello\\.");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("hello", m, re), false);
        EXPECT_EQ(std::regex_match("hello.", m, re), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_period2_end_benchmark)
{
    Regex<PosixExtended> re(".*hi... there$");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.search("Hello there", m), false);
        EXPECT_EQ(re.search("I said fyhi... there", m), true);
        EXPECT_EQ(re.search("....hi... ", m), false);
        EXPECT_EQ(re.search("I said fyhihii there", m), true);
        EXPECT_EQ(re.search("I said fyhihi there", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_period2_end_benchmark_reference_stdcpp)
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
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_plus_benchmark)
{
    Regex<PosixExtended> re("a+");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.search("b", m), false);
        EXPECT_EQ(re.search("a", m), true);
        EXPECT_EQ(re.search("aaaaaabbbbb", m), true);
        EXPECT_EQ(re.search("aaaaaaaaaaa", m), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_plus_benchmark_reference_stdcpp)
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
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_questionmark_benchmark)
{
    Regex<PosixExtended> re("da?d");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.search("a", m), false);
        EXPECT_EQ(re.search("daa", m), false);
        EXPECT_EQ(re.search("ddddd", m), true);
        EXPECT_EQ(re.search("dd", m), true);
        EXPECT_EQ(re.search("dad", m), true);
        EXPECT_EQ(re.search("dada", m), true);
        EXPECT_EQ(re.search("adadaa", m), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_questionmark_benchmark_reference_stdcpp)
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
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(character_class_benchmark)
{
    Regex<PosixExtended> re("[[:alpha:]]");
    RegexResult m;
    String haystack = "[Window]\nOpacity=255\nAudibleBeep=0\n";

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match(haystack.characters(), m), false);
        EXPECT_EQ(re.search(haystack.characters(), m), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(character_class_benchmark_reference_stdcpp)
{
    std::regex re("[[:alpha:]]");
    std::cmatch m;
    String haystack = "[Window]\nOpacity=255\nAudibleBeep=0\n";

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match(haystack.characters(), m, re), false);
        EXPECT_EQ(std::regex_search(haystack.characters(), m, re), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(escaped_char_questionmark_benchmark)
{
    Regex<PosixExtended> re("This\\.?And\\.?That");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("ThisAndThat", m), true);
        EXPECT_EQ(re.match("This.And.That", m), true);
        EXPECT_EQ(re.match("This And That", m), false);
        EXPECT_EQ(re.match("This..And..That", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(escaped_char_questionmark_benchmark_reference_stdcpp)
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
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(char_qualifier_asterisk_benchmark)
{
    Regex<PosixExtended> re("regex*");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.search("#include <regex.h>", m), true);
        EXPECT_EQ(re.search("#include <stdio.h>", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(char_qualifier_asterisk_benchmark_reference_stdcpp)
{
    std::regex re("regex*");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_search("#include <regex.h>", m, re), true);
        EXPECT_EQ(std::regex_search("#include <stdio.h>", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(parens_qualifier_questionmark_benchmark)
{
    Regex<PosixExtended> re("test(hello)?test");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("testtest", m), true);
        EXPECT_EQ(re.match("testhellotest", m), true);
        EXPECT_EQ(re.match("testasfdtest", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(parens_qualifier_questionmark_benchmark_reference_stdcpp)
{
    std::regex re("test(hello)?test");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("testtest", m, re), true);
        EXPECT_EQ(std::regex_match("testhellotest", m, re), true);
        EXPECT_EQ(std::regex_match("testasfdtest", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(parens_qualifier_asterisk_benchmark)
{
    Regex<PosixExtended> re("test(hello)*test");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("testtest", m), true);
        EXPECT_EQ(re.match("testhellohellotest", m), true);
        EXPECT_EQ(re.search("testhellohellotest, testhellotest", m), true);
        EXPECT_EQ(re.match("aaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(parens_qualifier_asterisk_benchmark_reference_stdcpp)
{
    std::regex re("test(hello)*test");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("testtest", m, re), true);
        EXPECT_EQ(std::regex_match("testhellohellotest", m, re), true);
        EXPECT_EQ(std::regex_search("testhellohellotest, testhellotest", m, re), true);
        EXPECT_EQ(std::regex_match("aaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(parens_qualifier_asterisk_2_benchmark)
{
    Regex<PosixExtended> re("test(.*)test");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("testasdftest", m), true);
        EXPECT_EQ(re.match("testasdfasdftest", m), true);
        EXPECT_EQ(re.search("testaaaatest, testbbbtest, testtest", m), true);
        EXPECT_EQ(re.match("aaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(parens_qualifier_asterisk_2_benchmark_reference_stdcpp)
{
    std::regex re("test(.*)test");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("testasdftest", m, re), true);
        EXPECT_EQ(std::regex_match("testasdfasdftest", m, re), true);
        EXPECT_EQ(std::regex_search("testaaaatest, testbbbtest, testtest", m, re), true);
        EXPECT_EQ(std::regex_match("aaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(multi_parens_qualifier_questionmark_benchmark)
{
    Regex<PosixExtended> re("test(a)?(b)?(c)?test");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("testtest", m), true);
        EXPECT_EQ(re.match("testabctest", m), true);
        EXPECT_EQ(re.search("testabctest, testactest", m), true);
        EXPECT_EQ(re.match("aaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb", m), false);
        EXPECT_EQ(re.match("test", m), false);
        EXPECT_EQ(re.match("whaaaaat", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(multi_parens_qualifier_questionmark_benchmark_reference_stdcpp)
{
    std::regex re("test(a)?(b)?(c)?test");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("testtest", m, re), true);
        EXPECT_EQ(std::regex_match("testabctest", m, re), true);
        EXPECT_EQ(std::regex_search("testabctest, testactest", m, re), true);
        EXPECT_EQ(std::regex_match("aaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb", m, re), false);
        EXPECT_EQ(std::regex_match("test", m, re), false);
        EXPECT_EQ(std::regex_match("whaaaaat", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_alternative_benchmark)
{
    Regex<PosixExtended> re("test|hello|friends");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("test", m), true);
        EXPECT_EQ(re.match("hello", m), true);
        EXPECT_EQ(re.match("friends", m), true);
        EXPECT_EQ(re.match("whaaaaat", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_alternative_benchmark_reference_stdcpp)
{
    std::regex re("test|hello|friends");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("test", m, re), true);
        EXPECT_EQ(std::regex_match("hello", m, re), true);
        EXPECT_EQ(std::regex_match("friends", m, re), true);
        EXPECT_EQ(std::regex_match("whaaaaat", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(alternative_match_groups_benchmark)
{
    Regex<PosixExtended> re("test(a)?(b)?|hello ?(dear|my)? friends");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("test", m), true);
        EXPECT_EQ(re.match("testa", m), true);
        EXPECT_EQ(re.match("testb", m), true);
        EXPECT_EQ(re.match("hello friends", m), true);
        EXPECT_EQ(re.match("hello dear friends", m), true);
        EXPECT_EQ(re.match("hello my friends", m), true);
        EXPECT_EQ(re.match("testabc", m), false);
        EXPECT_EQ(re.match("hello test friends", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(alternative_match_groups_benchmark_reference_stdcpp)
{
    std::regex re("test(a)?(b)?|hello ?(dear|my)? friends");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("test", m, re), true);
        EXPECT_EQ(std::regex_match("testa", m, re), true);
        EXPECT_EQ(std::regex_match("testb", m, re), true);
        EXPECT_EQ(std::regex_match("hello friends", m, re), true);
        EXPECT_EQ(std::regex_match("hello dear friends", m, re), true);
        EXPECT_EQ(std::regex_match("hello my friends", m, re), true);
        EXPECT_EQ(std::regex_match("testabc", m, re), false);
        EXPECT_EQ(std::regex_match("hello test friends", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(parens_qualifier_exact_benchmark)
{
    Regex<PosixExtended> re("(hello){3}");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("hello", m), false);
        EXPECT_EQ(re.match("hellohellohello", m), true);
        EXPECT_EQ(re.search("hellohellohellohello", m), true);
        EXPECT_EQ(re.search("test hellohellohello", m), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(parens_qualifier_exact_benchmark_reference_stdcpp)
{
    std::regex re("(hello){3}");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("hello", m, re), false);
        EXPECT_EQ(std::regex_match("hellohellohello", m, re), true);
        EXPECT_EQ(std::regex_search("hellohellohellohello", m, re), true);
        EXPECT_EQ(std::regex_search("test hellohellohello", m, re), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(parens_qualifier_minimum_benchmark)
{
    Regex<PosixExtended> re("(hello){3,}");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("hello", m), false);
        EXPECT_EQ(re.match("hellohellohello", m), true);
        EXPECT_EQ(re.search("hellohellohellohello", m), true);
        EXPECT_EQ(re.search("test hellohellohello", m), true);
        EXPECT_EQ(re.search("test hellohellohellohello", m), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(parens_qualifier_minimum_benchmark_reference_stdcpp)
{
    std::regex re("(hello){3,}");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("hello", m, re), false);
        EXPECT_EQ(std::regex_match("hellohellohello", m, re), true);
        EXPECT_EQ(std::regex_search("hellohellohellohello", m, re), true);
        EXPECT_EQ(std::regex_search("test hellohellohello", m, re), true);
        EXPECT_EQ(std::regex_search("test hellohellohellohello", m, re), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(parens_qualifier_maximum_benchmark)
{
    Regex<PosixExtended> re("(hello){2,3}");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("hello", m), false);
        EXPECT_EQ(re.match("hellohellohello", m), true);
        EXPECT_EQ(re.search("hellohellohellohello", m), true);
        EXPECT_EQ(re.search("test hellohellohello", m), true);
        EXPECT_EQ(re.search("test hellohellohellohello", m), true);
        EXPECT_EQ(re.match("test hellohellohellohello", m), false);
        EXPECT_EQ(re.search("test hellohellohellohello", m), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(parens_qualifier_maximum_benchmark_reference_stdcpp)
{
    std::regex re("(hello){2,3}");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("hello", m, re), false);
        EXPECT_EQ(std::regex_match("hellohellohello", m, re), true);
        EXPECT_EQ(std::regex_search("hellohellohellohello", m, re), true);
        EXPECT_EQ(std::regex_search("test hellohellohello", m, re), true);
        EXPECT_EQ(std::regex_search("test hellohellohellohello", m, re), true);
        EXPECT_EQ(std::regex_match("test hellohellohellohello", m, re), false);
        EXPECT_EQ(std::regex_search("test hellohellohellohello", m, re), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(char_qualifier_min_max_benchmark)
{
    Regex<PosixExtended> re("c{3,30}");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("cc", m), false);
        EXPECT_EQ(re.match("ccc", m), true);
        EXPECT_EQ(re.match("cccccccccccccccccccccccccccccc", m), true);
        EXPECT_EQ(re.match("ccccccccccccccccccccccccccccccc", m), false);
        EXPECT_EQ(re.search("ccccccccccccccccccccccccccccccc", m), true);
        EXPECT_EQ(re.match("cccccccccccccccccccccccccccccccc", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(char_qualifier_min_max_benchmark_reference_stdcpp)
{
    std::regex re("c{3,30}");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("cc", m, re), false);
        EXPECT_EQ(std::regex_match("ccc", m, re), true);
        EXPECT_EQ(std::regex_match("cccccccccccccccccccccccccccccc", m, re), true);
        EXPECT_EQ(std::regex_match("ccccccccccccccccccccccccccccccc", m, re), false);
        EXPECT_EQ(std::regex_search("ccccccccccccccccccccccccccccccc", m, re), true);
        EXPECT_EQ(std::regex_match("cccccccccccccccccccccccccccccccc", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_bracket_chars_benchmark)
{
    Regex<PosixExtended> re("[abc]");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("a", m), true);
        EXPECT_EQ(re.match("b", m), true);
        EXPECT_EQ(re.match("c", m), true);
        EXPECT_EQ(re.match("d", m), false);
        EXPECT_EQ(re.match("e", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_bracket_chars_benchmark_reference_stdcpp)
{
    std::regex re("[abc]");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("a", m, re), true);
        EXPECT_EQ(std::regex_match("b", m, re), true);
        EXPECT_EQ(std::regex_match("c", m, re), true);
        EXPECT_EQ(std::regex_match("d", m, re), false);
        EXPECT_EQ(std::regex_match("e", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_bracket_chars_inverse_benchmark)
{
    Regex<PosixExtended> re("[^abc]");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("a", m), false);
        EXPECT_EQ(re.match("b", m), false);
        EXPECT_EQ(re.match("c", m), false);
        EXPECT_EQ(re.match("d", m), true);
        EXPECT_EQ(re.match("e", m), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_bracket_chars_inverse_benchmark_reference_stdcpp)
{
    std::regex re("[^abc]");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("a", m, re), false);
        EXPECT_EQ(std::regex_match("b", m, re), false);
        EXPECT_EQ(std::regex_match("c", m, re), false);
        EXPECT_EQ(std::regex_match("d", m, re), true);
        EXPECT_EQ(std::regex_match("e", m, re), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_bracket_chars_range_benchmark)
{
    Regex<PosixExtended> re("[a-d]");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("a", m), true);
        EXPECT_EQ(re.match("b", m), true);
        EXPECT_EQ(re.match("c", m), true);
        EXPECT_EQ(re.match("d", m), true);
        EXPECT_EQ(re.match("e", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_bracket_chars_range_benchmark_reference_stdcpp)
{
    std::regex re("[a-d]");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("a", m, re), true);
        EXPECT_EQ(std::regex_match("b", m, re), true);
        EXPECT_EQ(std::regex_match("c", m, re), true);
        EXPECT_EQ(std::regex_match("d", m, re), true);
        EXPECT_EQ(std::regex_match("e", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_bracket_chars_range_inverse_benchmark)
{
    Regex<PosixExtended> re("[^a-df-z]");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("a", m), false);
        EXPECT_EQ(re.match("b", m), false);
        EXPECT_EQ(re.match("c", m), false);
        EXPECT_EQ(re.match("d", m), false);
        EXPECT_EQ(re.match("e", m), true);
        EXPECT_EQ(re.match("k", m), false);
        EXPECT_EQ(re.match("z", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_bracket_chars_range_inverse_benchmark_reference_stdcpp)
{
    std::regex re("[^a-df-z]");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("a", m, re), false);
        EXPECT_EQ(std::regex_match("b", m, re), false);
        EXPECT_EQ(std::regex_match("c", m, re), false);
        EXPECT_EQ(std::regex_match("d", m, re), false);
        EXPECT_EQ(std::regex_match("e", m, re), true);
        EXPECT_EQ(std::regex_match("k", m, re), false);
        EXPECT_EQ(std::regex_match("z", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(bracket_character_class_uuid_benchmark)
{
    Regex<PosixExtended> re("^([[:xdigit:]]{8})-([[:xdigit:]]{4})-([[:xdigit:]]{4})-([[:xdigit:]]{4})-([[:xdigit:]]{12})$");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("fb9b62a2-1579-4e3a-afba-76239ccb6583", m), true);
        EXPECT_EQ(re.match("fb9b62a2", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(bracket_character_class_uuid_benchmark_reference_stdcpp)
{
    std::regex re("^([[:xdigit:]]{8})-([[:xdigit:]]{4})-([[:xdigit:]]{4})-([[:xdigit:]]{4})-([[:xdigit:]]{12})$");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("fb9b62a2-1579-4e3a-afba-76239ccb6583", m, re), true);
        EXPECT_EQ(std::regex_match("fb9b62a2", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_bracket_character_class_inverse_benchmark)
{
    Regex<PosixExtended> re("[^[:digit:]]");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("1", m), false);
        EXPECT_EQ(re.match("2", m), false);
        EXPECT_EQ(re.match("3", m), false);
        EXPECT_EQ(re.match("d", m), true);
        EXPECT_EQ(re.match("e", m), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_bracket_character_class_inverse_benchmark_reference_stdcpp)
{
    std::regex re("[^[:digit:]]");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("1", m, re), false);
        EXPECT_EQ(std::regex_match("2", m, re), false);
        EXPECT_EQ(std::regex_match("3", m, re), false);
        EXPECT_EQ(std::regex_match("d", m, re), true);
        EXPECT_EQ(std::regex_match("e", m, re), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(email_address_benchmark)
{
    Regex<PosixExtended> re("^[A-Z0-9a-z._%+-]{1,64}@(?:[A-Za-z0-9-]{1,63}\\.){1,125}[A-Za-z]{2,63}$");
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("hello.world@domain.tld", m), true);
        EXPECT_EQ(re.match("this.is.a.very_long_email_address@world.wide.web", m), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(email_address_benchmark_reference_stdcpp)
{
    std::regex re("^[A-Z0-9a-z._%+-]{1,64}@(?:[A-Za-z0-9-]{1,63}\\.){1,125}[A-Za-z]{2,63}$");
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("hello.world@domain.tld", m, re), true);
        EXPECT_EQ(std::regex_match("this.is.a.very_long_email_address@world.wide.web", m, re), true);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_ignorecase_benchmark)
{
    Regex<PosixExtended> re("^hello friends", PosixFlags::Insensitive);
    RegexResult m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(re.match("Hello Friends", m), true);
        EXPECT_EQ(re.match("hello Friends", m), true);

        EXPECT_EQ(re.match("hello Friends!", m), false);
        EXPECT_EQ(re.search("hello Friends", m), true);

        EXPECT_EQ(re.match("hell Friends", m), false);
        EXPECT_EQ(re.search("hell Friends", m), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_ignorecase_benchmark_reference_stdcpp)
{
    std::regex re("^hello friends", std::regex_constants::icase);
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("Hello Friends", m, re), true);
        EXPECT_EQ(std::regex_match("hello Friends", m, re), true);

        EXPECT_EQ(std::regex_match("hello Friends!", m, re), false);
        EXPECT_EQ(std::regex_search("hello Friends", m, re), true);

        EXPECT_EQ(std::regex_match("hell Friends", m, re), false);
        EXPECT_EQ(std::regex_search("hell Friends", m, re), false);
    }
}
#    endif

#    if defined(REGEX_BENCHMARK_OUR)
BENCHMARK_CASE(simple_notbol_noteol_benchmark)
{
    String pattern = "^hello friends$";
    String pattern2 = "hello friends";
    regex_t regex, regex2;

    EXPECT_EQ(regcomp(&regex, pattern.characters(), REG_EXTENDED | REG_NOSUB | REG_ICASE), REG_NOERR);
    EXPECT_EQ(regcomp(&regex2, pattern2.characters(), REG_EXTENDED | REG_NOSUB | REG_ICASE), REG_NOERR);

    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {

        EXPECT_EQ(regexec(&regex, "hello friends", 0, NULL, REG_NOTBOL), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hello friends", 0, NULL, REG_NOTEOL), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hello friends", 0, NULL, REG_NOTBOL | REG_NOTEOL), REG_NOMATCH);

        EXPECT_EQ(regexec(&regex, "a hello friends b", 0, NULL, REG_NOTBOL), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "a hello friends", 0, NULL, REG_NOTBOL), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "a hello friends", 0, NULL, REG_NOTBOL | REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "a hello friends b", 0, NULL, REG_NOTBOL | REG_SEARCH), REG_NOERR);

        EXPECT_EQ(regexec(&regex, "a hello friends b", 0, NULL, REG_NOTEOL), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hello friends b", 0, NULL, REG_NOTEOL), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "hello friends b", 0, NULL, REG_NOTEOL | REG_SEARCH), REG_NOERR);
        EXPECT_EQ(regexec(&regex, "a hello friends b", 0, NULL, REG_NOTEOL | REG_SEARCH), REG_NOMATCH);

        EXPECT_EQ(regexec(&regex, "a hello friends b", 0, NULL, REG_NOTBOL | REG_NOTEOL), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex, "a hello friends b", 0, NULL, REG_NOTBOL | REG_NOTEOL | REG_SEARCH), REG_NOMATCH);

        EXPECT_EQ(regexec(&regex2, "hello friends", 0, NULL, REG_NOTBOL), REG_NOMATCH);
        EXPECT_EQ(regexec(&regex2, "hello friends", 0, NULL, REG_NOTEOL), REG_NOMATCH);
    }

    regfree(&regex);
}
#    endif

#    if defined(REGEX_BENCHMARK_OTHER)
BENCHMARK_CASE(simple_notbol_noteol_benchmark_reference_stdcpp)
{
    std::regex re1("^hello friends$", std::regex_constants::match_not_bol);
    std::regex re2("^hello friends$", std::regex_constants::match_not_eol);
    std::regex re3("^hello friends$", std::regex_constants::match_not_bol | std::regex_constants::match_not_eol);
    std::regex re4("hello friends", std::regex_constants::match_not_bol);
    std::regex re5("hello friends", std::regex_constants::match_not_eol);
    std::cmatch m;
    for (size_t i = 0; i < BENCHMARK_LOOP_ITERATIONS; ++i) {
        EXPECT_EQ(std::regex_match("hello friends", m, re1), false);
        EXPECT_EQ(std::regex_match("hello friends", m, re2), false);
        EXPECT_EQ(std::regex_match("hello friends", m, re3), false);

        EXPECT_EQ(std::regex_match("a hello friends b", m, re1), false);
        EXPECT_EQ(std::regex_match("a hello friends", m, re1), false);
        EXPECT_EQ(std::regex_search("a hello friends", m, re1), true);
        EXPECT_EQ(std::regex_search("a hello friends b", m, re1), true);

        EXPECT_EQ(std::regex_match("a hello friends b", m, re2), false);
        EXPECT_EQ(std::regex_match("hello friends b", m, re2), false);
        EXPECT_EQ(std::regex_search("hello friends b", m, re2), true);
        EXPECT_EQ(std::regex_search("a hello friends b", m, re2), false);

        EXPECT_EQ(std::regex_match("a hello friends b", m, re3), false);
        EXPECT_EQ(std::regex_search("a hello friends b", m, re3), false);

        EXPECT_EQ(std::regex_match("hello friends", m, re4), false);
        EXPECT_EQ(std::regex_match("hello friends", m, re5), false);
    }
}
#    endif

#endif
