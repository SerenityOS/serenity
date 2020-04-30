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

#include <AK/RegexParser.h>
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
    PosixExtendedParser p(l);

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
    PosixExtendedParser p(l);

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

TEST_MAIN(Regex)
