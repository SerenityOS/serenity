#include <AK/TestSuite.h>

#include <AK/BinarySearch.h>

TEST_CASE(vector_ints)
{
    Vector<int> ints;
    ints.append(1);
    ints.append(2);
    ints.append(3);

    auto test1 = *binary_search(ints.data(), ints.size(), 1, AK::integral_compare<int>);
    auto test2 = *binary_search(ints.data(), ints.size(), 2, AK::integral_compare<int>);
    auto test3 = *binary_search(ints.data(), ints.size(), 3, AK::integral_compare<int>);
    EXPECT_EQ(test1, 1);
    EXPECT_EQ(test2, 2);
    EXPECT_EQ(test3, 3);
}

TEST_CASE(array_doubles)
{
    double doubles[] = { 1.1, 9.9, 33.33 };

    auto test1 = *binary_search(doubles, 3, 1.1, AK::integral_compare<double>);
    auto test2 = *binary_search(doubles, 3, 9.9, AK::integral_compare<double>);
    auto test3 = *binary_search(doubles, 3, 33.33, AK::integral_compare<double>);
    EXPECT_EQ(test1, 1.1);
    EXPECT_EQ(test2, 9.9);
    EXPECT_EQ(test3, 33.33);
}

TEST_CASE(vector_strings)
{
    Vector<String> strings;
    strings.append("bat");
    strings.append("cat");
    strings.append("dog");

    auto string_compare = [](const String& a, const String& b) -> int {
        return strcmp(a.characters(), b.characters());
    };
    auto test1 = *binary_search(strings.data(), strings.size(), String("bat"), string_compare);
    auto test2 = *binary_search(strings.data(), strings.size(), String("cat"), string_compare);
    auto test3 = *binary_search(strings.data(), strings.size(), String("dog"), string_compare);
    EXPECT_EQ(test1, String("bat"));
    EXPECT_EQ(test2, String("cat"));
    EXPECT_EQ(test3, String("dog"));
}

TEST_CASE(single_element)
{
    Vector<int> ints;
    ints.append(1);

    auto test1 = *binary_search(ints.data(), ints.size(), 1, AK::integral_compare<int>);
    EXPECT_EQ(test1, 1);
}

TEST_CASE(not_found)
{
    Vector<int> ints;
    ints.append(1);
    ints.append(2);
    ints.append(3);

    auto test1 = binary_search(ints.data(), ints.size(), -1, AK::integral_compare<int>);
    auto test2 = binary_search(ints.data(), ints.size(), 0, AK::integral_compare<int>);
    auto test3 = binary_search(ints.data(), ints.size(), 4, AK::integral_compare<int>);
    EXPECT_EQ(test1, nullptr);
    EXPECT_EQ(test2, nullptr);
    EXPECT_EQ(test3, nullptr);
}

TEST_CASE(no_elements)
{
    Vector<int> ints;

    auto test1 = binary_search(ints.data(), ints.size(), 1, AK::integral_compare<int>);
    EXPECT_EQ(test1, nullptr);
}

TEST_MAIN(BinarySearch)
