/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/GenericLexer.h>
#include <AK/StringView.h>

TEST_CASE(should_constexpr_construct_from_empty_string_view)
{
    constexpr GenericLexer sut(StringView {});
    static_assert(sut.is_eof());
}

TEST_CASE(should_construct_from_string_view)
{
    constexpr GenericLexer sut(StringView { "abcdef" });
    static_assert(!sut.is_eof());
}

TEST_CASE(should_constexpr_tell)
{
    constexpr GenericLexer sut(StringView { "abcdef" });
    static_assert(sut.tell() == 0);
}

TEST_CASE(should_constexpr_tell_remaining)
{
    constexpr GenericLexer sut(StringView { "abcdef" });
    static_assert(sut.tell_remaining() == 6);
}

TEST_CASE(should_constexpr_peek)
{
    constexpr GenericLexer sut(StringView { "abcdef" });
    static_assert(sut.peek() == 'a');
    static_assert(sut.peek(2) == 'c');
    static_assert(sut.peek(100) == '\0');
}

TEST_CASE(should_constexpr_next_is)
{
    constexpr GenericLexer sut(StringView { "abcdef" });
    static_assert(sut.next_is('a'));
    static_assert(sut.next_is("abc"));
    static_assert(sut.next_is(StringView { "abc" }));
}

TEST_CASE(should_constexpr_retreat)
{
    constexpr auto sut = [] {
        GenericLexer sut(StringView { "abcdef" });
        sut.consume();
        sut.retreat();
        return sut;
    }();
    static_assert(sut.peek() == 'a');
}

TEST_CASE(should_constexpr_consume_1)
{
    constexpr auto sut = [] {
        GenericLexer sut(StringView { "abcdef" });
        sut.consume();
        return sut;
    }();
    static_assert(sut.peek() == 'b');
}

TEST_CASE(should_constexpr_consume_specific_char)
{
    constexpr auto sut = [] {
        GenericLexer sut(StringView { "abcdef" });
        sut.consume_specific('a');
        return sut;
    }();
    static_assert(sut.peek() == 'b');
}

TEST_CASE(should_constexpr_consume_specific_string_view)
{
    constexpr auto sut = [] {
        GenericLexer sut(StringView { "abcdef" });
        sut.consume_specific(StringView { "ab" });
        return sut;
    }();
    static_assert(sut.peek() == 'c');
}

TEST_CASE(should_constexpr_consume_specific_cstring)
{
    constexpr auto sut = [] {
        GenericLexer sut(StringView { "abcdef" });
        sut.consume_specific("abcd");
        return sut;
    }();
    static_assert(sut.peek() == 'e');
}

TEST_CASE(should_constexpr_ignore_until)
{
    constexpr auto sut = [] {
        GenericLexer sut(StringView { "abcdef" });
        sut.ignore_until('d');
        return sut;
    }();
    static_assert(sut.peek() == 'e');
}

TEST_CASE(should_constexpr_ignore_until_cstring)
{
    constexpr auto sut = [] {
        GenericLexer sut(StringView { "abcdef" });
        sut.ignore_until("cde");
        return sut;
    }();
    static_assert(sut.peek() == 'f');
}

TEST_CASE(should_constexpr_next_is_pred)
{
    constexpr auto pred = [](auto c) {
        return c == 'a';
    };
    constexpr GenericLexer sut(StringView { "abcdef" });
    static_assert(sut.next_is(pred));
}

TEST_CASE(should_constexpr_ignore_while_pred)
{
    constexpr auto sut = [] {
        constexpr auto pred = [](auto c) {
            return c == 'a';
        };

        GenericLexer sut(StringView { "abcdef" });
        sut.ignore_while(pred);
        return sut;
    }();
    static_assert(sut.peek() == 'b');
}

TEST_CASE(should_constexpr_ignore_until_pred)
{
    constexpr auto sut = [] {
        constexpr auto pred = [](auto c) {
            return c == 'c';
        };

        GenericLexer sut(StringView { "abcdef" });
        sut.ignore_until(pred);
        return sut;
    }();
    static_assert(sut.peek() == 'c');
}
