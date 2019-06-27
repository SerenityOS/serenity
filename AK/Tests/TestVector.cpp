#include "TestHelpers.h"
#include <AK/AKString.h>
#include <AK/Vector.h>

int main()
{
    EXPECT(Vector<int>().is_empty());
    EXPECT(Vector<int>().size() == 0);

    Vector<int> ints;
    ints.append(1);
    ints.append(2);
    ints.append(3);
    EXPECT_EQ(ints.size(), 3);
    EXPECT_EQ(ints.take_last(), 3);
    EXPECT_EQ(ints.size(), 2);
    EXPECT_EQ(ints.take_last(), 2);
    EXPECT_EQ(ints.size(), 1);
    EXPECT_EQ(ints.take_last(), 1);
    EXPECT_EQ(ints.size(), 0);

    ints.clear();
    EXPECT_EQ(ints.size(), 0);

    Vector<String> strings;
    strings.append("ABC");
    strings.append("DEF");

    int loop_counter = 0;
    for (const String& string : strings) {
        EXPECT(!string.is_null());
        EXPECT(!string.is_empty());
        ++loop_counter;
    }

    loop_counter = 0;
    for (auto& string : (const_cast<const Vector<String>&>(strings))) {
        EXPECT(!string.is_null());
        EXPECT(!string.is_empty());
        ++loop_counter;
    }
    EXPECT_EQ(loop_counter, 2);

    return 0;
}
