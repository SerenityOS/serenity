#include <AK/TestSuite.h>

#include <AK/String.h>

TEST_CASE(construct_empty)
{
    EXPECT(String().is_null());
    EXPECT(String().is_empty());
    EXPECT(!String().characters());

    EXPECT(!String("").is_null());
    EXPECT(String("").is_empty());
    EXPECT(String("").characters() != nullptr);

    EXPECT(String("").impl() == String::empty().impl());
}

TEST_CASE(construct_contents)
{
    String test_string = "ABCDEF";
    EXPECT(!test_string.is_empty());
    EXPECT(!test_string.is_null());
    EXPECT_EQ(test_string.length(), 6u);
    EXPECT_EQ(test_string.length(), strlen(test_string.characters()));
    EXPECT(test_string.characters() != nullptr);
    EXPECT(!strcmp(test_string.characters(), "ABCDEF"));

    EXPECT(test_string == "ABCDEF");
    EXPECT(test_string != "ABCDE");
    EXPECT(test_string != "ABCDEFG");
}

TEST_CASE(compare)
{
    String test_string = "ABCDEF";
    EXPECT("a" < String("b"));
    EXPECT(!("a" > String("b")));
    EXPECT("b" > String("a"));
    EXPECT(!("b" < String("b")));
    EXPECT("a" >= String("a"));
    EXPECT(!("a" >= String("b")));
    EXPECT("a" <= String("a"));
    EXPECT(!("b" <= String("a")));
}

TEST_CASE(index_access)
{
    String test_string = "ABCDEF";
    EXPECT_EQ(test_string[0], 'A');
    EXPECT_EQ(test_string[1], 'B');
}

TEST_CASE(starts_with)
{
    String test_string = "ABCDEF";
    EXPECT(test_string.starts_with("AB"));
    EXPECT(test_string.starts_with("ABCDEF"));
    EXPECT(!test_string.starts_with("DEF"));
}

TEST_CASE(ends_with)
{
    String test_string = "ABCDEF";
    EXPECT(test_string.ends_with("EF"));
    EXPECT(test_string.ends_with("ABCDEF"));
    EXPECT(!test_string.ends_with("ABC"));
}

TEST_CASE(copy_string)
{
    String test_string = "ABCDEF";
    auto test_string_copy = test_string;
    EXPECT_EQ(test_string, test_string_copy);
    EXPECT_EQ(test_string.characters(), test_string_copy.characters());
}

TEST_CASE(move_string)
{
    String test_string = "ABCDEF";
    auto test_string_copy = test_string;
    auto test_string_move = move(test_string_copy);
    EXPECT_EQ(test_string, test_string_move);
    EXPECT(test_string_copy.is_null());
}

TEST_CASE(repeated)
{
    EXPECT_EQ(String::repeated('x', 0), "");
    EXPECT_EQ(String::repeated('x', 1), "x");
    EXPECT_EQ(String::repeated('x', 2), "xx");
}

TEST_CASE(to_int)
{
    bool ok;
    EXPECT(String("123").to_int(ok) == 123 && ok);
    EXPECT(String("-123").to_int(ok) == -123 && ok);
}

TEST_CASE(to_lowercase)
{
    EXPECT(String("ABC").to_lowercase() == "abc");
}

TEST_CASE(to_uppercase)
{
    EXPECT(String("AbC").to_uppercase() == "ABC");
}

TEST_MAIN(String)
