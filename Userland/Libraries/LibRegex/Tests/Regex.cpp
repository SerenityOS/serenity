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

#include <AK/TestSuite.h> // import first, to prevent warning of VERIFY* redefinition

#include <AK/StringBuilder.h>
#include <LibRegex/Regex.h>
#include <LibRegex/RegexDebug.h>
#include <stdio.h>

static ECMAScriptOptions match_test_api_options(const ECMAScriptOptions options)
{
    return options;
}

static PosixOptions match_test_api_options(const PosixOptions options)
{
    return options;
}

TEST_CASE(regex_options_ecmascript)
{
    ECMAScriptOptions eo;
    eo |= ECMAScriptFlags::Global;

    EXPECT(eo & ECMAScriptFlags::Global);
    EXPECT(!(eo & ECMAScriptFlags::Insensitive));

    eo = match_test_api_options(ECMAScriptFlags::Global | ECMAScriptFlags::Insensitive | ECMAScriptFlags::Sticky);
    EXPECT(eo & ECMAScriptFlags::Global);
    EXPECT(eo & ECMAScriptFlags::Insensitive);
    EXPECT(eo & ECMAScriptFlags::Sticky);
    EXPECT(!(eo & ECMAScriptFlags::Unicode));
    EXPECT(!(eo & ECMAScriptFlags::Multiline));
    EXPECT(!(eo & ECMAScriptFlags::SingleLine));

    eo &= ECMAScriptFlags::Insensitive;
    EXPECT(!(eo & ECMAScriptFlags::Global));
    EXPECT(eo & ECMAScriptFlags::Insensitive);
    EXPECT(!(eo & ECMAScriptFlags::Multiline));

    eo &= ECMAScriptFlags::Sticky;
    EXPECT(!(eo & ECMAScriptFlags::Global));
    EXPECT(!(eo & ECMAScriptFlags::Insensitive));
    EXPECT(!(eo & ECMAScriptFlags::Multiline));
    EXPECT(!(eo & ECMAScriptFlags::Sticky));

    eo = ~ECMAScriptFlags::Insensitive;
    EXPECT(eo & ECMAScriptFlags::Global);
    EXPECT(!(eo & ECMAScriptFlags::Insensitive));
    EXPECT(eo & ECMAScriptFlags::Multiline);
    EXPECT(eo & ECMAScriptFlags::Sticky);
}

TEST_CASE(regex_options_posix)
{
    PosixOptions eo;
    eo |= PosixFlags::Global;

    EXPECT(eo & PosixFlags::Global);
    EXPECT(!(eo & PosixFlags::Insensitive));

    eo = match_test_api_options(PosixFlags::Global | PosixFlags::Insensitive | PosixFlags::MatchNotBeginOfLine);
    EXPECT(eo & PosixFlags::Global);
    EXPECT(eo & PosixFlags::Insensitive);
    EXPECT(eo & PosixFlags::MatchNotBeginOfLine);
    EXPECT(!(eo & PosixFlags::Unicode));
    EXPECT(!(eo & PosixFlags::Multiline));

    eo &= PosixFlags::Insensitive;
    EXPECT(!(eo & PosixFlags::Global));
    EXPECT(eo & PosixFlags::Insensitive);
    EXPECT(!(eo & PosixFlags::Multiline));

    eo &= PosixFlags::MatchNotBeginOfLine;
    EXPECT(!(eo & PosixFlags::Global));
    EXPECT(!(eo & PosixFlags::Insensitive));
    EXPECT(!(eo & PosixFlags::Multiline));

    eo = ~PosixFlags::Insensitive;
    EXPECT(eo & PosixFlags::Global);
    EXPECT(!(eo & PosixFlags::Insensitive));
    EXPECT(eo & PosixFlags::Multiline);
}

TEST_CASE(regex_lexer)
{
    Lexer l("/[.*+?^${}()|[\\]\\\\]/g");
    EXPECT(l.next().type() == regex::TokenType::Slash);
    EXPECT(l.next().type() == regex::TokenType::LeftBracket);
    EXPECT(l.next().type() == regex::TokenType::Period);
    EXPECT(l.next().type() == regex::TokenType::Asterisk);
    EXPECT(l.next().type() == regex::TokenType::Plus);
    EXPECT(l.next().type() == regex::TokenType::Questionmark);
    EXPECT(l.next().type() == regex::TokenType::Circumflex);
    EXPECT(l.next().type() == regex::TokenType::Dollar);
    EXPECT(l.next().type() == regex::TokenType::LeftCurly);
    EXPECT(l.next().type() == regex::TokenType::RightCurly);
    EXPECT(l.next().type() == regex::TokenType::LeftParen);
    EXPECT(l.next().type() == regex::TokenType::RightParen);
    EXPECT(l.next().type() == regex::TokenType::Pipe);
    EXPECT(l.next().type() == regex::TokenType::LeftBracket);
    EXPECT(l.next().type() == regex::TokenType::EscapeSequence);
    EXPECT(l.next().type() == regex::TokenType::EscapeSequence);
    EXPECT(l.next().type() == regex::TokenType::RightBracket);
    EXPECT(l.next().type() == regex::TokenType::Slash);
    EXPECT(l.next().type() == regex::TokenType::Char);
}

TEST_CASE(parser_error_parens)
{
    String pattern = "test()test";
    Lexer l(pattern);
    PosixExtendedParser p(l);
    p.parse();
    EXPECT(p.has_error());
    EXPECT(p.error() == Error::EmptySubExpression);
}

TEST_CASE(parser_error_special_characters_used_at_wrong_place)
{
    String pattern;
    Vector<char, 5> chars = { '*', '+', '?', '{' };
    StringBuilder b;

    Lexer l;
    PosixExtended p(l);

    for (auto& ch : chars) {
        // First in ere
        b.clear();
        b.append(ch);
        pattern = b.build();
        l.set_source(pattern);
        p.parse();
        EXPECT(p.has_error());
        EXPECT(p.error() == Error::InvalidRepetitionMarker);

        // After vertical line
        b.clear();
        b.append("a|");
        b.append(ch);
        pattern = b.build();
        l.set_source(pattern);
        p.parse();
        EXPECT(p.has_error());
        EXPECT(p.error() == Error::InvalidRepetitionMarker);

        // After circumflex
        b.clear();
        b.append("^");
        b.append(ch);
        pattern = b.build();
        l.set_source(pattern);
        p.parse();
        EXPECT(p.has_error());
        EXPECT(p.error() == Error::InvalidRepetitionMarker);

        // After dollar
        b.clear();
        b.append("$");
        b.append(ch);
        pattern = b.build();
        l.set_source(pattern);
        p.parse();
        EXPECT(p.has_error());
        EXPECT(p.error() == Error::InvalidRepetitionMarker);

        // After left parens
        b.clear();
        b.append("(");
        b.append(ch);
        b.append(")");
        pattern = b.build();
        l.set_source(pattern);
        p.parse();
        EXPECT(p.has_error());
        EXPECT(p.error() == Error::InvalidRepetitionMarker);
    }
}

TEST_CASE(parser_error_vertical_line_used_at_wrong_place)
{
    Lexer l;
    PosixExtended p(l);

    // First in ere
    l.set_source("|asdf");
    p.parse();
    EXPECT(p.has_error());
    EXPECT(p.error() == Error::EmptySubExpression);

    // Last in ere
    l.set_source("asdf|");
    p.parse();
    EXPECT(p.has_error());
    EXPECT(p.error() == Error::EmptySubExpression);

    // After left parens
    l.set_source("(|asdf)");
    p.parse();
    EXPECT(p.has_error());
    EXPECT(p.error() == Error::EmptySubExpression);

    // Proceed right parens
    l.set_source("(asdf)|");
    p.parse();
    EXPECT(p.has_error());
    EXPECT(p.error() == Error::EmptySubExpression);
}

TEST_CASE(catch_all_first)
{
    Regex<PosixExtended> re("^.*$");
    RegexResult m;
    re.match("Hello World", m);
    EXPECT(m.count == 1);
    EXPECT(re.match("Hello World", m));
}

TEST_CASE(catch_all)
{
    Regex<PosixExtended> re("^.*$", PosixFlags::Global);

    EXPECT(re.has_match("Hello World"));
    EXPECT(re.match("Hello World").success);
    EXPECT(re.match("Hello World").count == 1);

    EXPECT(has_match("Hello World", re));
    auto res = match("Hello World", re);
    EXPECT(res.success);
    EXPECT(res.count == 1);
    EXPECT(res.matches.size() == 1);
    EXPECT(res.matches.first().view == "Hello World");
}

TEST_CASE(catch_all_again)
{
    Regex<PosixExtended> re("^.*$", PosixFlags::Extra);
    EXPECT_EQ(has_match("Hello World", re), true);
}

TEST_CASE(char_utf8)
{
    Regex<PosixExtended> re("😀");
    RegexResult result;

    EXPECT_EQ((result = match("Привет, мир! 😀 γειά σου κόσμος 😀 こんにちは世界", re, PosixFlags::Global)).success, true);
    EXPECT_EQ(result.count, 2u);
}

TEST_CASE(catch_all_newline)
{
    Regex<PosixExtended> re("^.*$", PosixFlags::Multiline | PosixFlags::StringCopyMatches);
    RegexResult result;
    auto lambda = [&result, &re]() {
        String aaa = "Hello World\nTest\n1234\n";
        result = match(aaa, re);
        EXPECT_EQ(result.success, true);
    };
    lambda();
    EXPECT_EQ(result.count, 3u);
    EXPECT_EQ(result.matches.at(0).view, "Hello World");
    EXPECT_EQ(result.matches.at(1).view, "Test");
    EXPECT_EQ(result.matches.at(2).view, "1234");
}

TEST_CASE(catch_all_newline_view)
{
    Regex<PosixExtended> re("^.*$", PosixFlags::Multiline);
    RegexResult result;

    String aaa = "Hello World\nTest\n1234\n";
    result = match(aaa, re);
    EXPECT_EQ(result.success, true);
    EXPECT_EQ(result.count, 3u);
    String str = "Hello World";
    EXPECT_EQ(result.matches.at(0).view, str.view());
    EXPECT_EQ(result.matches.at(1).view, "Test");
    EXPECT_EQ(result.matches.at(2).view, "1234");
}

TEST_CASE(catch_all_newline_2)
{
    Regex<PosixExtended> re("^.*$");
    RegexResult result;
    result = match("Hello World\nTest\n1234\n", re, PosixFlags::Multiline | PosixFlags::StringCopyMatches);
    EXPECT_EQ(result.success, true);
    EXPECT_EQ(result.count, 3u);
    EXPECT_EQ(result.matches.at(0).view, "Hello World");
    EXPECT_EQ(result.matches.at(1).view, "Test");
    EXPECT_EQ(result.matches.at(2).view, "1234");

    result = match("Hello World\nTest\n1234\n", re);
    EXPECT_EQ(result.success, true);
    EXPECT_EQ(result.count, 1u);
    EXPECT_EQ(result.matches.at(0).view, "Hello World\nTest\n1234\n");
}

TEST_CASE(match_all_character_class)
{
    Regex<PosixExtended> re("[[:alpha:]]");
    String str = "[Window]\nOpacity=255\nAudibleBeep=0\n";
    RegexResult result = match(str, re, PosixFlags::Global | PosixFlags::StringCopyMatches);

    EXPECT_EQ(result.success, true);
    EXPECT_EQ(result.count, 24u);
    EXPECT_EQ(result.matches.at(0).view, "W");
    EXPECT_EQ(result.matches.at(1).view, "i");
    EXPECT_EQ(result.matches.at(2).view, "n");
    EXPECT(&result.matches.at(0).view.characters_without_null_termination()[0] != &str.view().characters_without_null_termination()[1]);
}

TEST_CASE(match_character_class_with_assertion)
{
    Regex<PosixExtended> re("[[:alpha:]]+$");
    String str = "abcdef";
    RegexResult result = match(str, re);

    EXPECT_EQ(result.success, true);
    EXPECT_EQ(result.count, 1u);
}

TEST_CASE(example_for_git_commit)
{
    Regex<PosixExtended> re("^.*$");
    auto result = re.match("Well, hello friends!\nHello World!");

    EXPECT(result.success);
    EXPECT(result.count == 1);
    EXPECT(result.matches.at(0).view.starts_with("Well"));
    EXPECT(result.matches.at(0).view.length() == 33);

    EXPECT(re.has_match("Well,...."));

    result = re.match("Well, hello friends!\nHello World!", PosixFlags::Multiline);

    EXPECT(result.success);
    EXPECT(result.count == 2);
    EXPECT(result.matches.at(0).view == "Well, hello friends!");
    EXPECT(result.matches.at(1).view == "Hello World!");
}

TEST_CASE(email_address)
{
    Regex<PosixExtended> re("^[A-Z0-9a-z._%+-]{1,64}@([A-Za-z0-9-]{1,63}\\.){1,125}[A-Za-z]{2,63}$");
    EXPECT(re.has_match("hello.world@domain.tld"));
    EXPECT(re.has_match("this.is.a.very_long_email_address@world.wide.web"));
}

TEST_CASE(ini_file_entries)
{
    Regex<PosixExtended> re("[[:alpha:]]*=([[:digit:]]*)|\\[(.*)\\]");
    RegexResult result;

#if REGEX_DEBUG
    RegexDebug regex_dbg(stderr);
    regex_dbg.print_raw_bytecode(re);
    regex_dbg.print_header();
    regex_dbg.print_bytecode(re);
#endif

    String haystack = "[Window]\nOpacity=255\nAudibleBeep=0\n";
    EXPECT_EQ(re.search(haystack.view(), result, PosixFlags::Multiline), true);
    EXPECT_EQ(result.count, 3u);

#if REGEX_DEBUG
    for (auto& v : result.matches)
        fprintf(stderr, "%s\n", v.view.to_string().characters());
#endif

    EXPECT_EQ(result.matches.at(0).view, "[Window]");
    EXPECT_EQ(result.capture_group_matches.at(0).at(0).view, "Window");
    EXPECT_EQ(result.matches.at(1).view, "Opacity=255");
    EXPECT_EQ(result.matches.at(1).line, 1u);
    EXPECT_EQ(result.matches.at(1).column, 0u);
    EXPECT_EQ(result.capture_group_matches.at(1).at(0).view, "255");
    EXPECT_EQ(result.capture_group_matches.at(1).at(0).line, 1u);
    EXPECT_EQ(result.capture_group_matches.at(1).at(0).column, 8u);
    EXPECT_EQ(result.matches.at(2).view, "AudibleBeep=0");
    EXPECT_EQ(result.capture_group_matches.at(2).at(0).view, "0");
    EXPECT_EQ(result.capture_group_matches.at(2).at(0).line, 2u);
    EXPECT_EQ(result.capture_group_matches.at(2).at(0).column, 12u);
}

TEST_CASE(ini_file_entries2)
{
    Regex<PosixExtended> re("[[:alpha:]]*=([[:digit:]]*)");
    RegexResult result;

    String haystack = "ViewMode=Icon";

    EXPECT_EQ(re.match(haystack.view(), result), false);
    EXPECT_EQ(result.count, 0u);

    EXPECT_EQ(re.search(haystack.view(), result), true);
    EXPECT_EQ(result.count, 1u);
}

TEST_CASE(named_capture_group)
{
    Regex<PosixExtended> re("[[:alpha:]]*=(?<Test>[[:digit:]]*)");
    RegexResult result;

#if REGEX_DEBUG
    RegexDebug regex_dbg(stderr);
    regex_dbg.print_raw_bytecode(re);
    regex_dbg.print_header();
    regex_dbg.print_bytecode(re);
#endif

    String haystack = "[Window]\nOpacity=255\nAudibleBeep=0\n";
    EXPECT_EQ(re.search(haystack, result, PosixFlags::Multiline), true);
    EXPECT_EQ(result.count, 2u);
    EXPECT_EQ(result.matches.at(0).view, "Opacity=255");
    EXPECT_EQ(result.named_capture_group_matches.at(0).ensure("Test").view, "255");
    EXPECT_EQ(result.matches.at(1).view, "AudibleBeep=0");
    EXPECT_EQ(result.named_capture_group_matches.at(1).ensure("Test").view, "0");
}

TEST_CASE(a_star)
{
    Regex<PosixExtended> re("a*");
    RegexResult result;

#if REGEX_DEBUG
    RegexDebug regex_dbg(stderr);
    regex_dbg.print_raw_bytecode(re);
    regex_dbg.print_header();
    regex_dbg.print_bytecode(re);
#endif

    String haystack = "[Window]\nOpacity=255\nAudibleBeep=0\n";
    EXPECT_EQ(re.search(haystack.view(), result, PosixFlags::Multiline), true);
    EXPECT_EQ(result.count, 32u);
    EXPECT_EQ(result.matches.at(0).view.length(), 0u);
    EXPECT_EQ(result.matches.at(10).view.length(), 1u);
    EXPECT_EQ(result.matches.at(10).view, "a");
    EXPECT_EQ(result.matches.at(31).view.length(), 0u);
}

TEST_CASE(simple_period_end_benchmark)
{
    Regex<PosixExtended> re("hello.$");
    RegexResult m;
    EXPECT_EQ(re.search("Hello1", m), false);
    EXPECT_EQ(re.search("hello1hello1", m), true);
    EXPECT_EQ(re.search("hello2hell", m), false);
    EXPECT_EQ(re.search("hello?", m), true);
}

TEST_CASE(ECMA262_parse)
{
    struct _test {
        const char* pattern;
        regex::Error expected_error { regex::Error::NoError };
        regex::ECMAScriptFlags flags {};
    };

    constexpr _test tests[] {
        { "^hello.$" },
        { "^(hello.)$" },
        { "^h{0,1}ello.$" },
        { "^hello\\W$" },
        { "^hell\\w.$" },
        { "^hell\\x6f1$" }, // ^hello1$
        { "^hel(?:l\\w).$" },
        { "^hel(?<LO>l\\w).$" },
        { "^[-a-zA-Z\\w\\s]+$" },
        { "\\bhello\\B" },
        { "^[\\w+/_-]+[=]{0,2}$" },                        // #4189
        { "^(?:[^<]*(<[\\w\\W]+>)[^>]*$|#([\\w\\-]*)$)" }, // #4189
        { "\\/" },                                         // #4189
        { ",/=-:" },                                       // #4243
        { "\\x" },                                         // Even invalid escapes are allowed if ~unicode.
        { "\\", regex::Error::InvalidTrailingEscape },
        { "(?", regex::Error::InvalidCaptureGroup },
        { "\\u1234", regex::Error::NoError, regex::ECMAScriptFlags::Unicode },
        { "[\\u1234]", regex::Error::NoError, regex::ECMAScriptFlags::Unicode },
        { ",(?", regex::Error::InvalidCaptureGroup }, // #4583
        { "{1}", regex::Error::InvalidPattern },
        { "{1,2}", regex::Error::InvalidPattern },
    };

    for (auto& test : tests) {
        Regex<ECMA262> re(test.pattern);
        EXPECT_EQ(re.parser_result.error, test.expected_error);
#if REGEX_DEBUG
        dbgln("\n");
        RegexDebug regex_dbg(stderr);
        regex_dbg.print_raw_bytecode(re);
        regex_dbg.print_header();
        regex_dbg.print_bytecode(re);
        dbgln("\n");
#endif
    }
}

TEST_CASE(ECMA262_match)
{
    struct _test {
        const char* pattern;
        const char* subject;
        bool matches { true };
        ECMAScriptFlags options {};
    };
    // clang-format off
    constexpr _test tests[] {
        { "^hello.$", "hello1" },
        { "^(hello.)$", "hello1" },
        { "^h{0,1}ello.$", "ello1" },
        { "^hello\\W$", "hello!" },
        { "^hell\\w.$", "hellx!" },
        { "^hell\\x6f1$", "hello1" },
        { "^hel(?<LO>l.)1$", "hello1" },
        { "^hel(?<LO>l.)1*\\k<LO>.$", "hello1lo1" },
        { "^[-a-z1-3\\s]+$", "hell2 o1" },
        { .pattern = "\\bhello\\B", .subject = "hello1", .options = ECMAScriptFlags::Global },
        { "\\b.*\\b", "hello1" },
        { "[^\\D\\S]{2}", "1 " },
        { "bar(?=f.)foo", "barfoo" },
        { "bar(?=foo)bar", "barbar", false },
        { "bar(?!foo)bar", "barbar", true },
        { "bar(?!bar)bar", "barbar", false },
        { "bar.*(?<=foo)", "barbar", false },
        { "bar.*(?<!foo)", "barbar", true },
        { "((...)X)+", "fooXbarXbazX", true },
        { "(?:)", "", true },
        // ECMA262, B.1.4. Regular Expression Pattern extensions for browsers
        { "{", "{", true, ECMAScriptFlags::BrowserExtended },
        { "\\5", "\5", true, ECMAScriptFlags::BrowserExtended },
        { "\\05", "\5", true, ECMAScriptFlags::BrowserExtended },
        { "\\455", "\45""5", true, ECMAScriptFlags::BrowserExtended },
        { "\\314", "\314", true, ECMAScriptFlags::BrowserExtended },
        { "\\cf", "\06", true, ECMAScriptFlags::BrowserExtended },
        { "\\c1", "\\c1", true, ECMAScriptFlags::BrowserExtended },
        { "[\\c1]", "\x11", true, ECMAScriptFlags::BrowserExtended },
        { "[\\w-\\d]", "-", true, ECMAScriptFlags::BrowserExtended },
        { "^(?:^^\\.?|[!+-]|!=|!==|#|%|%=|&|&&|&&=|&=|\\(|\\*|\\*=|\\+=|,|-=|->|\\/|\\/=|:|::|;|<|<<|<<=|<=|=|==|===|>|>=|>>|>>=|>>>|>>>=|[?@[^]|\\^=|\\^\\^|\\^\\^=|{|\\||\\|=|\\|\\||\\|\\|=|~|break|case|continue|delete|do|else|finally|instanceof|return|throw|try|typeof)\\s*(\\/(?=[^*/])(?:[^/[\\\\]|\\\\[\\S\\s]|\\[(?:[^\\\\\\]]|\\\\[\\S\\s])*(?:]|$))+\\/)",
                 "return /xx/", true, ECMAScriptFlags::BrowserExtended
        }, // #5517, appears to be matching JS expressions that involve regular expressions...
    };
    // clang-format on

    for (auto& test : tests) {
        Regex<ECMA262> re(test.pattern, test.options);
#if REGEX_DEBUG
        dbgln("\n");
        RegexDebug regex_dbg(stderr);
        regex_dbg.print_raw_bytecode(re);
        regex_dbg.print_header();
        regex_dbg.print_bytecode(re);
        dbgln("\n");
#endif
        EXPECT_EQ(re.parser_result.error, Error::NoError);
        EXPECT_EQ(re.match(test.subject).success, test.matches);
    }
}

TEST_CASE(replace)
{
    struct _test {
        const char* pattern;
        const char* replacement;
        const char* subject;
        const char* expected;
        ECMAScriptFlags options {};
    };

    constexpr _test tests[] {
        { "foo(.+)", "aaa", "test", "test" },
        { "foo(.+)", "test\\1", "foobar", "testbar" },
        { "foo(.+)", "\\2\\1", "foobar", "\\2bar" },
        { "foo(.+)", "\\\\\\1", "foobar", "\\bar" },
        { "foo(.)", "a\\1", "fooxfooy", "axay", ECMAScriptFlags::Multiline },
    };

    for (auto& test : tests) {
        Regex<ECMA262> re(test.pattern, test.options);
#if REGEX_DEBUG
        dbgln("\n");
        RegexDebug regex_dbg(stderr);
        regex_dbg.print_raw_bytecode(re);
        regex_dbg.print_header();
        regex_dbg.print_bytecode(re);
        dbgln("\n");
#endif
        EXPECT_EQ(re.parser_result.error, Error::NoError);
        EXPECT_EQ(re.replace(test.subject, test.replacement), test.expected);
    }
}

TEST_MAIN(Regex)
