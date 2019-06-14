#include <AK/AKString.h>

int main()
{
    ASSERT(String().is_null());
    ASSERT(String().is_empty());

    ASSERT(!String("").is_null());
    ASSERT(String("").is_empty());

    String test_string = "ABCDEF";
    ASSERT(!test_string.is_empty());
    ASSERT(!test_string.is_null());
    ASSERT(test_string.length() == 6);
    ASSERT(test_string.length() == strlen(test_string.characters()));
    ASSERT(!strcmp(test_string.characters(), "ABCDEF"));

    return 0;
}
