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

#include <AK/TestSuite.h> // import first, to prevent warning of ASSERT* redefinition

#include <AK/Regex.h>
#include <AK/StringBuilder.h>
#include <stdio.h>

ECMAScriptOptions match_test_api_options(const ECMAScriptOptions options)
{
    return options;
}
PosixOptions match_test_api_options(const PosixOptions options)
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

    eo = match_test_api_options(PosixFlags::Global | PosixFlags::Insensitive | PosixFlags::Anchored);
    EXPECT(eo & PosixFlags::Global);
    EXPECT(eo & PosixFlags::Insensitive);
    EXPECT(eo & PosixFlags::Anchored);
    EXPECT(!(eo & PosixFlags::Unicode));
    EXPECT(!(eo & PosixFlags::Multiline));

    eo &= PosixFlags::Insensitive;
    EXPECT(!(eo & PosixFlags::Global));
    EXPECT(eo & PosixFlags::Insensitive);
    EXPECT(!(eo & PosixFlags::Multiline));

    eo &= PosixFlags::Anchored;
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
    EXPECT(l.next().type() == AK::regex::TokenType::Slash);
    EXPECT(l.next().type() == AK::regex::TokenType::LeftBracket);
    EXPECT(l.next().type() == AK::regex::TokenType::Period);
    EXPECT(l.next().type() == AK::regex::TokenType::Asterisk);
    EXPECT(l.next().type() == AK::regex::TokenType::Plus);
    EXPECT(l.next().type() == AK::regex::TokenType::Questionmark);
    EXPECT(l.next().type() == AK::regex::TokenType::Circumflex);
    EXPECT(l.next().type() == AK::regex::TokenType::Dollar);
    EXPECT(l.next().type() == AK::regex::TokenType::LeftCurly);
    EXPECT(l.next().type() == AK::regex::TokenType::RightCurly);
    EXPECT(l.next().type() == AK::regex::TokenType::LeftParen);
    EXPECT(l.next().type() == AK::regex::TokenType::RightParen);
    EXPECT(l.next().type() == AK::regex::TokenType::Pipe);
    EXPECT(l.next().type() == AK::regex::TokenType::LeftBracket);
    EXPECT(l.next().type() == AK::regex::TokenType::EscapeSequence);
    EXPECT(l.next().type() == AK::regex::TokenType::EscapeSequence);
    EXPECT(l.next().type() == AK::regex::TokenType::RightBracket);
    EXPECT(l.next().type() == AK::regex::TokenType::Slash);
    EXPECT(l.next().type() == AK::regex::TokenType::OrdinaryCharacter);
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

TEST_CASE(catch_all)
{
    Regex<PosixExtended> regex("^.*$", PosixFlags::Global);

    EXPECT(regex.has_match("Hello World"));
    EXPECT(regex.match("Hello World").success);
    EXPECT(regex.match("Hello World").count == 1);

    EXPECT(has_match("Hello World", regex));
    auto res = match("Hello World", regex);
    EXPECT(res.success);
    EXPECT(res.count == 1);
    EXPECT(res.matches.size() == 1);
    EXPECT(res.matches.first().view == "Hello World");
}

TEST_CASE(catch_all_again)
{
    Regex<PosixExtended> regex("^.*$", PosixFlags::Extra);
    EXPECT_EQ(has_match("Hello World", regex), true);
}

TEST_CASE(char_utf8)
{
    Regex<PosixExtended> regex("😀");
    RegexResult result;

    EXPECT_EQ((result = match("Привет, мир! 😀 γειά σου κόσμος 😀 こんにちは世界", regex, PosixFlags::Global)).success, true);
    EXPECT_EQ(result.count, 2u);
}

TEST_CASE(catch_all_newline)
{
    Regex<PosixExtended> regex("^.*$", PosixFlags::Multiline | PosixFlags::StringCopyMatches);
    RegexResult result;
    auto lambda = [&result, &regex]() {
        String aaa = "Hello World\nTest\n1234\n";
        result = match(aaa, regex);
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
    Regex<PosixExtended> regex("^.*$", PosixFlags::Multiline);
    RegexResult result;

    String aaa = "Hello World\nTest\n1234\n";
    result = match(aaa, regex);
    EXPECT_EQ(result.success, true);
    EXPECT_EQ(result.count, 3u);
    String str = "Hello World";
    EXPECT_EQ(result.matches.at(0).view, str.view());
    EXPECT_EQ(result.matches.at(1).view, "Test");
    EXPECT_EQ(result.matches.at(2).view, "1234");
}

TEST_CASE(catch_all_newline_2)
{
    Regex<PosixExtended> regex("^.*$");
    RegexResult result;
    result = match("Hello World\nTest\n1234\n", regex, PosixFlags::Multiline | PosixFlags::StringCopyMatches);
    EXPECT_EQ(result.success, true);
    EXPECT_EQ(result.count, 3u);
    EXPECT_EQ(result.matches.at(0).view, "Hello World");
    EXPECT_EQ(result.matches.at(1).view, "Test");
    EXPECT_EQ(result.matches.at(2).view, "1234");

    result = match("Hello World\nTest\n1234\n", regex);
    EXPECT_EQ(result.success, true);
    EXPECT_EQ(result.count, 1u);
    EXPECT_EQ(result.matches.at(0).view, "Hello World\nTest\n1234\n");
}

TEST_CASE(match_all_character_class)
{
    Regex<PosixExtended> regex("[[:alpha:]]");
    String str = "[Window]\nOpacity=255\nAudibleBeep=0\n";
    RegexResult result = match(str, regex, PosixFlags::Global | PosixFlags::StringCopyMatches);

    EXPECT_EQ(result.success, true);
    EXPECT_EQ(result.count, 24u);
    EXPECT_EQ(result.matches.at(0).view, "W");
    EXPECT_EQ(result.matches.at(1).view, "i");
    EXPECT_EQ(result.matches.at(2).view, "n");
    EXPECT(&result.matches.at(0).view.characters_without_null_termination()[0] != &str.view().characters_without_null_termination()[1]);
}

TEST_CASE(example_for_git_commit)
{
    Regex<PosixExtended> regex("^.*$");
    auto result = regex.match("Well, hello friends!\nHello World!");

    EXPECT(result.success);
    EXPECT(result.count == 1);
    EXPECT(result.matches.at(0).view.starts_with("Well"));
    EXPECT(result.matches.at(0).view.length() == 33);

    EXPECT(regex.has_match("Well,...."));

    result = regex.match("Well, hello friends!\nHello World!", PosixFlags::Multiline);

    EXPECT(result.success);
    EXPECT(result.count == 2);
    EXPECT(result.matches.at(0).view == "Well, hello friends!");
    EXPECT(result.matches.at(1).view == "Hello World!");
}

TEST_CASE(email_address)
{
    Regex<PosixExtended> regex("^[A-Z0-9a-z._%+-]{1,64}@([A-Za-z0-9-]{1,63}\\.){1,125}[A-Za-z]{2,63}$");
    EXPECT(regex.has_match("hello.world@domain.tld"));
    EXPECT(regex.has_match("this.is.a.very_long_email_address@world.wide.web"));
}

TEST_CASE(ini_file_entries)
{
    Regex<PosixExtended> re("[[:alpha:]]*=([[:digit:]]*)|\\[(.*)\\]");
    RegexResult result;
    //    re.print_bytecode();

    String haystack = "[Window]\nOpacity=255\nAudibleBeep=0\n";
    EXPECT_EQ(re.search(haystack.view(), result, PosixFlags::Multiline), true);
    EXPECT_EQ(result.count, 3u);
    EXPECT_EQ(result.matches.at(0).view, "[Window]");
    EXPECT_EQ(result.capture_group_matches.at(0).at(1).view, "Window");
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

TEST_CASE(named_capture_group)
{
    Regex<PosixExtended> re("[[:alpha:]]*=(?<Test>[[:digit:]]*)");
    RegexResult result;

    String haystack = "[Window]\nOpacity=255\nAudibleBeep=0\n";
    EXPECT_EQ(re.search(haystack.view(), result, PosixFlags::Multiline), true);
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

    String haystack = "[Window]\nOpacity=255\nAudibleBeep=0\n";
    EXPECT_EQ(re.search(haystack.view(), result, PosixFlags::Multiline), true);
    EXPECT_EQ(result.count, 32u);
    EXPECT_EQ(result.matches.at(0).view.length(), 0u);
    EXPECT_EQ(result.matches.at(10).view.length(), 1u);
    EXPECT_EQ(result.matches.at(10).view, "a");
    EXPECT_EQ(result.matches.at(31).view.length(), 0u);
}

TEST_MAIN(Regex)
