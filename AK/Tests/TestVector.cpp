#include <AK/TestSuite.h>
#include <AK/AKString.h>
#include <AK/Vector.h>

TEST_CASE(construct)
{
    EXPECT(Vector<int>().is_empty());
    EXPECT(Vector<int>().size() == 0);
}

TEST_CASE(ints)
{
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
}

TEST_CASE(strings)
{
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
}

TEST_CASE(strings_insert_ordered)
{
    Vector<String> strings;
    strings.append("abc");
    strings.append("def");
    strings.append("ghi");

    strings.insert_before_matching("f-g", [](auto& entry) {
        return "f-g" < entry;
    });

    EXPECT_EQ(strings[0], "abc");
    EXPECT_EQ(strings[1], "def");
    EXPECT_EQ(strings[2], "f-g");
    EXPECT_EQ(strings[3], "ghi");
}

TEST_MAIN(Vector)
