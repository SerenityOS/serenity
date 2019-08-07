#include <AK/TestSuite.h>

#include <AK/AKString.h>
#include <AK/FixedArray.h>

TEST_CASE(construct)
{
    EXPECT(FixedArray<int>().size() == 0);
}

TEST_CASE(ints)
{
    FixedArray<int> ints(3);
    ints[0] = 0;
    ints[1] = 1;
    ints[2] = 2;
    EXPECT_EQ(ints[0], 0);
    EXPECT_EQ(ints[1], 1);
    EXPECT_EQ(ints[2], 2);

    ints.clear();
    EXPECT_EQ(ints.size(), 0u);
}

TEST_CASE(resize)
{
    FixedArray<String> strings(2);
    strings[0] = "ABC";
    strings[1] = "DEF";

    EXPECT_EQ(strings.size(), 2u);
    EXPECT_EQ(strings[0], "ABC");
    EXPECT_EQ(strings[1], "DEF");

    strings.resize(4);

    EXPECT_EQ(strings.size(), 4u);
    EXPECT_EQ(strings[0], "ABC");
    EXPECT_EQ(strings[1], "DEF");

    EXPECT_EQ(strings[2].is_null(), true);
    EXPECT_EQ(strings[3].is_null(), true);

    strings[2] = "GHI";
    strings[3] = "JKL";

    EXPECT_EQ(strings[2], "GHI");
    EXPECT_EQ(strings[3], "JKL");

    strings.resize(1);
    EXPECT_EQ(strings.size(), 1u);
    EXPECT_EQ(strings[0], "ABC");
}

TEST_MAIN(FixedArray)
