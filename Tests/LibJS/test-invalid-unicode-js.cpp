/*
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Parser.h>
#include <LibTest/TestCase.h>

TEST_CASE(invalid_unicode_only)
{
    char const* code = "\xEA\xFD";
    auto lexer = JS::Lexer(code);
    auto token = lexer.next();
    EXPECT_EQ(token.type(), JS::TokenType::Invalid);

    // After this we can get as many eof tokens as we like.
    for (auto i = 0; i < 10; i++) {
        auto eof_token = lexer.next();
        EXPECT_EQ(eof_token.type(), JS::TokenType::Eof);
    }
}

TEST_CASE(long_invalid_unicode)
{
    char const* code = "\xF7";
    auto lexer = JS::Lexer(code);
    auto token = lexer.next();
    EXPECT_EQ(token.type(), JS::TokenType::Invalid);

    // After this we can get as many eof tokens as we like.
    for (auto i = 0; i < 10; i++) {
        auto eof_token = lexer.next();
        EXPECT_EQ(eof_token.type(), JS::TokenType::Eof);
    }
}

TEST_CASE(invalid_unicode_and_valid_code)
{
    char const* code = "\xEA\xFDthrow 1;";
    auto lexer = JS::Lexer(code);
    auto invalid_token = lexer.next();
    EXPECT_EQ(invalid_token.type(), JS::TokenType::Invalid);
    // 0xEA is the start of a three character unicode code point thus it consumes the 't'.
    auto token_after = lexer.next();
    EXPECT_EQ(token_after.value(), "hrow");
}

TEST_CASE(long_invalid_unicode_and_valid_code)
{
    char const* code = "\xF7throw 1;";
    auto lexer = JS::Lexer(code);
    auto invalid_token = lexer.next();
    EXPECT_EQ(invalid_token.type(), JS::TokenType::Invalid);
    // 0xF7 is the start of a four character unicode code point thus it consumes 'thr'.
    auto token_after = lexer.next();
    EXPECT_EQ(token_after.value(), "ow");
}

TEST_CASE(invalid_unicode_after_valid_code_and_before_eof)
{
    char const* code = "let \xEA\xFD;";
    auto lexer = JS::Lexer(code);
    auto let_token = lexer.next();
    EXPECT_EQ(let_token.type(), JS::TokenType::Let);
    auto invalid_token = lexer.next();
    EXPECT_EQ(invalid_token.type(), JS::TokenType::Invalid);
    // It should still get the valid trivia in front.
    EXPECT_EQ(invalid_token.trivia(), " ");

    // After this we can get as many eof tokens as we like.
    for (auto i = 0; i < 10; i++) {
        auto eof_token = lexer.next();
        EXPECT_EQ(eof_token.type(), JS::TokenType::Eof);
    }
}
