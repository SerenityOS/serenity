#include <AK/TestSuite.h>

#include <AK/String.h>

TEST_CASE(construct_empty)
{
    EXPECT(StringView().is_null());
    EXPECT(StringView().is_empty());
    EXPECT(!StringView().characters_without_null_termination());
    EXPECT_EQ(StringView().length(), 0);
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

TEST_CASE(starts_with)
{
    String test_string = "ABCDEF";
    StringView test_string_view = test_string.view();
    EXPECT(test_string_view.starts_with("AB"));
    EXPECT(test_string_view.starts_with("ABCDEF"));
    EXPECT(!test_string_view.starts_with("DEF"));
}

TEST_CASE(ends_with)
{
    String test_string = "ABCDEF";
    StringView test_string_view = test_string.view();
    EXPECT(test_string_view.ends_with("DEF"));
    EXPECT(test_string_view.ends_with("ABCDEF"));
    EXPECT(!test_string_view.ends_with("ABCDE"));
    EXPECT(!test_string_view.ends_with("ABCDEFG"));
}

TEST_CASE(lines)
{
    String test_string = "a\nb\r\nc\rd";
    StringView test_string_view = test_string.view();
    Vector<StringView> test_string_vector = test_string_view.lines();
    EXPECT_EQ(test_string_vector.size(), 4);
    EXPECT(test_string_vector.at(0) == String("a"));
    EXPECT(test_string_vector.at(1) == String("b"));
    EXPECT(test_string_vector.at(2) == String("c"));
    EXPECT(test_string_vector.at(3) == String("d"));

    test_string = "```\nHello there\r\nHello there\n```";
    test_string_view = test_string.view();
    test_string_vector = test_string_view.lines();
    EXPECT_EQ(test_string_vector.size(), 4);
    EXPECT(test_string_vector.at(0) == String("```"));
    EXPECT(test_string_vector.at(1) == String("Hello there"));
    EXPECT(test_string_vector.at(2) == String("Hello there"));
    EXPECT(test_string_vector.at(3) == String("```"));

    test_string = "\n\n\n";
    test_string_view = test_string.view();
    test_string_vector = test_string_view.lines();
    EXPECT_EQ(test_string_vector.size(), 3);
    EXPECT_EQ(test_string_vector.at(0).is_empty(), true);
    EXPECT_EQ(test_string_vector.at(1).is_empty(), true);
    EXPECT_EQ(test_string_vector.at(2).is_empty(), true);
}

TEST_MAIN(StringView)
