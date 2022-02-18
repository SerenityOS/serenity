/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibCore/File.h>
#include <LibWeb/HTML/Parser/HTMLTokenizer.h>

using Tokenizer = Web::HTML::HTMLTokenizer;
using Token = Web::HTML::HTMLToken;

#define BEGIN_ENUMERATION(tokens)          \
    auto current_token = (tokens).begin(); \
    [[maybe_unused]] Token* last_token;

#define END_ENUMERATION() \
    EXPECT(current_token.is_end());

#define NEXT_TOKEN()              \
    last_token = &*current_token; \
    ++current_token;

#define EXPECT_START_TAG_TOKEN(_tag_name)                    \
    EXPECT_EQ(current_token->type(), Token::Type::StartTag); \
    EXPECT_EQ(current_token->tag_name(), #_tag_name);        \
    NEXT_TOKEN();

#define EXPECT_END_TAG_TOKEN(_tag_name)                    \
    EXPECT_EQ(current_token->type(), Token::Type::EndTag); \
    EXPECT_EQ(current_token->tag_name(), #_tag_name);      \
    NEXT_TOKEN();

#define EXPECT_END_OF_FILE_TOKEN()                            \
    EXPECT_EQ(current_token->type(), Token::Type::EndOfFile); \
    NEXT_TOKEN();

#define EXPECT_CHARACTER_TOKEN(character)                     \
    EXPECT_EQ(current_token->type(), Token::Type::Character); \
    EXPECT_EQ(current_token->code_point(), (u32)(character)); \
    NEXT_TOKEN();

#define EXPECT_CHARACTER_TOKENS(string)  \
    for (auto c : StringView(#string)) { \
        EXPECT_CHARACTER_TOKEN(c);       \
    }

#define EXPECT_COMMENT_TOKEN()                              \
    EXPECT_EQ(current_token->type(), Token::Type::Comment); \
    NEXT_TOKEN();

#define EXPECT_DOCTYPE_TOKEN()                              \
    EXPECT_EQ(current_token->type(), Token::Type::DOCTYPE); \
    NEXT_TOKEN();

#define EXPECT_TAG_TOKEN_ATTRIBUTE(name, value) \
    VERIFY(last_token);                         \
    EXPECT_EQ(last_token->attribute(#name), value);

#define EXPECT_TAG_TOKEN_ATTRIBUTE_COUNT(count) \
    VERIFY(last_token);                         \
    EXPECT_EQ(last_token->attribute_count(), (size_t)(count));

static Vector<Token> run_tokenizer(StringView input)
{
    Vector<Token> tokens;
    Tokenizer tokenizer { input, "UTF-8"sv };
    while (true) {
        auto maybe_token = tokenizer.next_token();
        if (!maybe_token.has_value())
            break;
        tokens.append(maybe_token.release_value());
    }
    return tokens;
}

// FIXME: It's not very nice to rely on the format of HTMLToken::to_string() to stay the same.
static u32 hash_tokens(Vector<Token> const& tokens)
{
    StringBuilder builder;
    for (auto& token : tokens)
        builder.append(token.to_string());
    return (u32)builder.string_view().hash();
}

TEST_CASE(empty)
{
    auto tokens = run_tokenizer("");
    BEGIN_ENUMERATION(tokens);
    EXPECT_END_OF_FILE_TOKEN();
    END_ENUMERATION();
}

TEST_CASE(basic)
{
    auto tokens = run_tokenizer("<html><head></head><body></body></html>");
    BEGIN_ENUMERATION(tokens);
    EXPECT_START_TAG_TOKEN(html);
    EXPECT_START_TAG_TOKEN(head);
    EXPECT_END_TAG_TOKEN(head);
    EXPECT_START_TAG_TOKEN(body);
    EXPECT_END_TAG_TOKEN(body);
    EXPECT_END_TAG_TOKEN(html);
    EXPECT_END_OF_FILE_TOKEN();
    END_ENUMERATION();
}

TEST_CASE(basic_with_text)
{
    auto tokens = run_tokenizer("<p>This is some text.</p>");
    BEGIN_ENUMERATION(tokens);
    EXPECT_START_TAG_TOKEN(p);
    EXPECT_CHARACTER_TOKENS(This is some text.);
    EXPECT_END_TAG_TOKEN(p);
    EXPECT_END_OF_FILE_TOKEN();
    END_ENUMERATION();
}

TEST_CASE(unquoted_attributes)
{
    auto tokens = run_tokenizer("<p foo=bar>");
    BEGIN_ENUMERATION(tokens);
    EXPECT_START_TAG_TOKEN(p);
    EXPECT_TAG_TOKEN_ATTRIBUTE_COUNT(1);
    EXPECT_TAG_TOKEN_ATTRIBUTE(foo, "bar");
    EXPECT_END_OF_FILE_TOKEN();
    END_ENUMERATION();
}

TEST_CASE(single_quoted_attributes)
{
    auto tokens = run_tokenizer("<p foo='bar'>");
    BEGIN_ENUMERATION(tokens);
    EXPECT_START_TAG_TOKEN(p);
    EXPECT_TAG_TOKEN_ATTRIBUTE_COUNT(1);
    EXPECT_TAG_TOKEN_ATTRIBUTE(foo, "bar");
    EXPECT_END_OF_FILE_TOKEN();
    END_ENUMERATION();
}

TEST_CASE(double_quoted_attributes)
{
    auto tokens = run_tokenizer("<p foo=\"bar\">");
    BEGIN_ENUMERATION(tokens);
    EXPECT_START_TAG_TOKEN(p);
    EXPECT_TAG_TOKEN_ATTRIBUTE_COUNT(1);
    EXPECT_TAG_TOKEN_ATTRIBUTE(foo, "bar");
    EXPECT_END_OF_FILE_TOKEN();
    END_ENUMERATION();
}

TEST_CASE(multiple_attributes)
{
    auto tokens = run_tokenizer("<p foo=\"bar\" baz=foobar foo2=\"bar2\">");
    BEGIN_ENUMERATION(tokens);
    EXPECT_START_TAG_TOKEN(p);
    EXPECT_TAG_TOKEN_ATTRIBUTE_COUNT(3);
    EXPECT_TAG_TOKEN_ATTRIBUTE(foo, "bar");
    EXPECT_TAG_TOKEN_ATTRIBUTE(baz, "foobar");
    EXPECT_TAG_TOKEN_ATTRIBUTE(foo2, "bar2");
    EXPECT_END_OF_FILE_TOKEN();
    END_ENUMERATION();
}

TEST_CASE(character_reference_in_attribute)
{
    auto tokens = run_tokenizer("<p foo=a&amp;b bar='a&#38;b' baz=\"a&#x26;b\">");
    BEGIN_ENUMERATION(tokens);
    EXPECT_START_TAG_TOKEN(p);
    EXPECT_TAG_TOKEN_ATTRIBUTE_COUNT(3);
    EXPECT_TAG_TOKEN_ATTRIBUTE(foo, "a&b");
    EXPECT_TAG_TOKEN_ATTRIBUTE(bar, "a&b");
    EXPECT_TAG_TOKEN_ATTRIBUTE(baz, "a&b");
    EXPECT_END_OF_FILE_TOKEN();
    END_ENUMERATION();
}

TEST_CASE(comment)
{
    auto tokens = run_tokenizer("<p><!-- This is a comment --></p>");
    BEGIN_ENUMERATION(tokens);
    EXPECT_START_TAG_TOKEN(p);
    EXPECT_COMMENT_TOKEN();
    EXPECT_END_TAG_TOKEN(p);
    EXPECT_END_OF_FILE_TOKEN();
    END_ENUMERATION();
}

TEST_CASE(doctype)
{
    auto tokens = run_tokenizer("<!DOCTYPE html><html></html>");
    BEGIN_ENUMERATION(tokens);
    EXPECT_DOCTYPE_TOKEN();
    EXPECT_START_TAG_TOKEN(html);
    EXPECT_END_TAG_TOKEN(html);
}

// NOTE: This relies on the format of HTMLToken::to_string() staying the same.
//       If that changes, or something is added to the test HTML, the hash needs to be adjusted.
TEST_CASE(regression)
{
    auto file = Core::File::open("/usr/Tests/LibWeb/tokenizer-test.html", Core::OpenMode::ReadOnly);
    VERIFY(!file.is_error());
    auto file_contents = file.value()->read_all();
    auto tokens = run_tokenizer(file_contents);
    u32 hash = hash_tokens(tokens);
    EXPECT_EQ(hash, 710375345u);
}
