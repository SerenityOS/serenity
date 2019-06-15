#include "TestHelpers.h"
#include <AK/AKString.h>

int main()
{
    EXPECT(String().is_null());
    EXPECT(String().is_empty());
    EXPECT(!String().characters());

    EXPECT(!String("").is_null());
    EXPECT(String("").is_empty());
    EXPECT(String("").characters());

    EXPECT(String("").impl() == String::empty().impl());

    String test_string = "ABCDEF";
    EXPECT(!test_string.is_empty());
    EXPECT(!test_string.is_null());
    EXPECT_EQ(test_string.length(), 6);
    EXPECT_EQ(test_string.length(), (int)strlen(test_string.characters()));
    EXPECT(test_string.characters());
    EXPECT(!strcmp(test_string.characters(), "ABCDEF"));

    EXPECT(test_string == "ABCDEF");
    EXPECT(test_string != "ABCDE");
    EXPECT(test_string != "ABCDEFG");

    EXPECT_EQ(test_string[0], 'A');
    EXPECT_EQ(test_string[1], 'B');

    EXPECT(test_string.starts_with("AB"));
    EXPECT(test_string.starts_with("ABCDEF"));
    EXPECT(!test_string.starts_with("DEF"));

    EXPECT(test_string.ends_with("EF"));
    EXPECT(test_string.ends_with("ABCDEF"));
    EXPECT(!test_string.ends_with("ABC"));

    auto test_string_copy = test_string;
    EXPECT_EQ(test_string, test_string_copy);
    EXPECT_EQ(test_string.characters(), test_string_copy.characters());

    auto test_string_move = move(test_string_copy);
    EXPECT_EQ(test_string, test_string_move);
    EXPECT(test_string_copy.is_null());

    EXPECT_EQ(String::repeated('x', 0), "");
    EXPECT_EQ(String::repeated('x', 1), "x");
    EXPECT_EQ(String::repeated('x', 2), "xx");

    bool ok;
    EXPECT(String("123").to_int(ok) == 123 && ok);
    EXPECT(String("-123").to_int(ok) == -123 && ok);

    EXPECT(String("ABC").to_lowercase() == "abc");
    EXPECT(String("AbC").to_uppercase() == "ABC");

    return 0;
}
