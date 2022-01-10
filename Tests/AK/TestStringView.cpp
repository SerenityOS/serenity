/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/String.h>
#include <AK/Vector.h>

TEST_CASE(construct_empty)
{
    EXPECT(StringView().is_null());
    EXPECT(StringView().is_empty());
    EXPECT(!StringView().characters_without_null_termination());
    EXPECT_EQ(StringView().length(), 0u);
}

TEST_CASE(view_literal)
{
    const char* truth = "cats rule dogs drool";
    StringView view(truth);
    EXPECT_EQ(view.is_null(), false);
    EXPECT_EQ(view.characters_without_null_termination(), truth);
    EXPECT_EQ(view, view);
    EXPECT_EQ(view, truth);
}

TEST_CASE(compare_views)
{
    String foo1 = "foo";
    String foo2 = "foo";
    auto view1 = foo1.view();
    auto view2 = foo2.view();

    EXPECT_EQ(view1, view2);
    EXPECT_EQ(view1, foo1);
    EXPECT_EQ(view1, foo2);
    EXPECT_EQ(view1, "foo");
}

TEST_CASE(string_view_literal_operator)
{
    StringView literal_view = "foo"sv;
    String test_string = "foo";

    EXPECT_EQ(literal_view.length(), test_string.length());
    EXPECT_EQ(literal_view, test_string);
}

TEST_CASE(starts_with)
{
    String test_string = "ABCDEF";
    StringView test_string_view = test_string.view();
    EXPECT(test_string_view.starts_with('A'));
    EXPECT(!test_string_view.starts_with('B'));
    EXPECT(test_string_view.starts_with("AB"));
    EXPECT(test_string_view.starts_with("ABCDEF"));
    EXPECT(!test_string_view.starts_with("DEF"));
    EXPECT(test_string_view.starts_with("abc", CaseSensitivity::CaseInsensitive));
    EXPECT(!test_string_view.starts_with("abc", CaseSensitivity::CaseSensitive));
}

TEST_CASE(ends_with)
{
    String test_string = "ABCDEF";
    StringView test_string_view = test_string.view();
    EXPECT(test_string_view.ends_with("DEF"));
    EXPECT(test_string_view.ends_with('F'));
    EXPECT(!test_string_view.ends_with('E'));
    EXPECT(test_string_view.ends_with("ABCDEF"));
    EXPECT(!test_string_view.ends_with("ABCDE"));
    EXPECT(!test_string_view.ends_with("ABCDEFG"));
    EXPECT(test_string_view.ends_with("def", CaseSensitivity::CaseInsensitive));
    EXPECT(!test_string_view.ends_with("def", CaseSensitivity::CaseSensitive));
}

TEST_CASE(lines)
{
    String test_string = "a\rb\nc\r\nd";
    StringView test_string_view = test_string.view();
    Vector<StringView> test_string_vector = test_string_view.lines();
    EXPECT_EQ(test_string_vector.size(), 4u);
    EXPECT(test_string_vector.at(0) == String("a"));
    EXPECT(test_string_vector.at(1) == String("b"));
    EXPECT(test_string_vector.at(2) == String("c"));
    EXPECT(test_string_vector.at(3) == String("d"));

    test_string = "```\nHello there\r\nHello there\n```";
    test_string_view = test_string.view();
    test_string_vector = test_string_view.lines();
    EXPECT_EQ(test_string_vector.size(), 4u);
    EXPECT(test_string_vector.at(0) == String("```"));
    EXPECT(test_string_vector.at(1) == String("Hello there"));
    EXPECT(test_string_vector.at(2) == String("Hello there"));
    EXPECT(test_string_vector.at(3) == String("```"));

    test_string = "\n\n\n";
    test_string_view = test_string.view();
    test_string_vector = test_string_view.lines();
    EXPECT_EQ(test_string_vector.size(), 3u);
    EXPECT_EQ(test_string_vector.at(0).is_empty(), true);
    EXPECT_EQ(test_string_vector.at(1).is_empty(), true);
    EXPECT_EQ(test_string_vector.at(2).is_empty(), true);
}

TEST_CASE(find)
{
    auto test_string_view = "aabbcc_xy_ccbbaa"sv;
    EXPECT_EQ(test_string_view.find('b'), 2U);
    EXPECT_EQ(test_string_view.find('_'), 6U);
    EXPECT_EQ(test_string_view.find('n').has_value(), false);
}

TEST_CASE(find_last)
{
    auto test_string_view = "aabbcc_xy_ccbbaa"sv;
    EXPECT_EQ(test_string_view.find_last('b'), 13U);
    EXPECT_EQ(test_string_view.find_last('_'), 9U);
    EXPECT_EQ(test_string_view.find_last('3').has_value(), false);

    test_string_view = "/"sv;
    EXPECT_EQ(test_string_view.find_last('/'), 0U);
}

TEST_CASE(find_any_of)
{
    auto test_string_view = "aabbcc_xy_ccbbaa"sv;
    EXPECT_EQ(test_string_view.find_any_of("bc", StringView::SearchDirection::Forward), 2U);
    EXPECT_EQ(test_string_view.find_any_of("yx", StringView::SearchDirection::Forward), 7U);
    EXPECT_EQ(test_string_view.find_any_of("defg", StringView::SearchDirection::Forward).has_value(), false);
    EXPECT_EQ(test_string_view.find_any_of("bc", StringView::SearchDirection::Backward), 13U);
    EXPECT_EQ(test_string_view.find_any_of("yx", StringView::SearchDirection::Backward), 8U);
    EXPECT_EQ(test_string_view.find_any_of("fghi", StringView::SearchDirection::Backward).has_value(), false);

    test_string_view = "/"sv;
    EXPECT_EQ(test_string_view.find_any_of("/", StringView::SearchDirection::Forward), 0U);
    EXPECT_EQ(test_string_view.find_any_of("/", StringView::SearchDirection::Backward), 0U);
}

TEST_CASE(split_view)
{
    StringView test_string_view = "axxbxcxd";
    EXPECT_EQ(test_string_view.split_view('x'), Vector<StringView>({ "a", "b", "c", "d" }));
    EXPECT_EQ(test_string_view.split_view('x', true), Vector<StringView>({ "a", "", "b", "c", "d" }));
    EXPECT_EQ(test_string_view.split_view("x"), Vector<StringView>({ "a", "b", "c", "d" }));
    EXPECT_EQ(test_string_view.split_view("x", true), Vector<StringView>({ "a", "", "b", "c", "d" }));

    test_string_view = "axxbx";
    EXPECT_EQ(test_string_view.split_view('x'), Vector<StringView>({ "a", "b" }));
    EXPECT_EQ(test_string_view.split_view('x', true), Vector<StringView>({ "a", "", "b", "" }));
    EXPECT_EQ(test_string_view.split_view("x"), Vector<StringView>({ "a", "b" }));
    EXPECT_EQ(test_string_view.split_view("x", true), Vector<StringView>({ "a", "", "b", "" }));

    test_string_view = "axxbcxxdxx";
    EXPECT_EQ(test_string_view.split_view("xx"), Vector<StringView>({ "a", "bc", "d" }));
    EXPECT_EQ(test_string_view.split_view("xx", true), Vector<StringView>({ "a", "bc", "d", "" }));

    test_string_view = "ax_b_cxd";
    Function<bool(char)> predicate = [](char ch) { return ch == 'x' || ch == '_'; };
    EXPECT_EQ(test_string_view.split_view_if(predicate), Vector<StringView>({ "a", "b", "c", "d" }));
    EXPECT_EQ(test_string_view.split_view_if(predicate, true), Vector<StringView>({ "a", "", "b", "c", "d" }));
    EXPECT_EQ(test_string_view.split_view_if(predicate), Vector<StringView>({ "a", "b", "c", "d" }));
    EXPECT_EQ(test_string_view.split_view_if(predicate, true), Vector<StringView>({ "a", "", "b", "c", "d" }));
}

TEST_CASE(constexpr_stuff)
{
#define do_test()                                                       \
    static_assert(test_constexpr.length() == 3);                        \
    static_assert(!test_constexpr.is_empty());                          \
    static_assert(test_constexpr.is_one_of("foo", "bar", "baz"));       \
    static_assert(test_constexpr.is_one_of("foo"sv, "bar"sv, "baz"sv)); \
    static_assert(test_constexpr != "fob"sv);                           \
    static_assert(test_constexpr != "fob");                             \
    static_assert(test_constexpr.substring_view(1).is_one_of("oo"sv));

    {
        // Can initialize from ""sv.
        constexpr StringView test_constexpr { "foo"sv };
        do_test();
    }

    {
        // Can initialize from char const*.
        constexpr StringView test_constexpr { "foo" };
        do_test();
    }
#undef do_test
}

TEST_CASE(case_insensitive_hash)
{
    auto string1 = "abcdef"sv;
    auto string2 = "ABCDEF"sv;
    auto string3 = "aBcDeF"sv;
    auto string4 = "foo"sv;

    EXPECT_EQ(CaseInsensitiveStringViewTraits::hash(string1), CaseInsensitiveStringViewTraits::hash(string2));
    EXPECT_EQ(CaseInsensitiveStringViewTraits::hash(string1), CaseInsensitiveStringViewTraits::hash(string3));
    EXPECT_NE(CaseInsensitiveStringViewTraits::hash(string1), CaseInsensitiveStringViewTraits::hash(string4));
}
