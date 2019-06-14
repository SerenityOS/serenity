#include <AK/AKString.h>

int main()
{
    ASSERT(String().is_null());
    ASSERT(String().is_empty());
    ASSERT(!String().characters());

    ASSERT(!String("").is_null());
    ASSERT(String("").is_empty());
    ASSERT(String("").characters());

    ASSERT(String("").impl() == String::empty().impl());

    String test_string = "ABCDEF";
    ASSERT(!test_string.is_empty());
    ASSERT(!test_string.is_null());
    ASSERT(test_string.length() == 6);
    ASSERT(test_string.length() == strlen(test_string.characters()));
    ASSERT(test_string.characters());
    ASSERT(!strcmp(test_string.characters(), "ABCDEF"));

    ASSERT(test_string == "ABCDEF");
    ASSERT(test_string != "ABCDE");
    ASSERT(test_string != "ABCDEFG");

    auto test_string_copy = test_string;
    ASSERT(test_string == test_string_copy);
    ASSERT(test_string.characters() == test_string_copy.characters());

    auto test_string_move = move(test_string_copy);
    ASSERT(test_string == test_string_move);
    ASSERT(test_string_copy.is_null());

    return 0;
}
