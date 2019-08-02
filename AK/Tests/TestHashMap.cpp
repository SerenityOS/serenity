#include <AK/TestSuite.h>

#include <AK/AKString.h>
#include <AK/HashMap.h>

TEST_CASE(construct)
{
    typedef HashMap<int, int> IntIntMap;
    EXPECT(IntIntMap().is_empty());
    EXPECT_EQ(IntIntMap().size(), 0);
}

TEST_CASE(populate)
{
    HashMap<int, String> number_to_string;
    number_to_string.set(1, "One");
    number_to_string.set(2, "Two");
    number_to_string.set(3, "Three");

    EXPECT_EQ(number_to_string.is_empty(), false);
    EXPECT_EQ(number_to_string.size(), 3);
}

TEST_CASE(range_loop)
{
    HashMap<int, String> number_to_string;
    number_to_string.set(1, "One");
    number_to_string.set(2, "Two");
    number_to_string.set(3, "Three");

    int loop_counter = 0;
    for (auto& it : number_to_string) {
        EXPECT_EQ(it.value.is_null(), false);
        ++loop_counter;
    }
    EXPECT_EQ(loop_counter, 3);
}

TEST_CASE(map_remove)
{
    HashMap<int, String> number_to_string;
    number_to_string.set(1, "One");
    number_to_string.set(2, "Two");
    number_to_string.set(3, "Three");

    number_to_string.remove(1);
    EXPECT_EQ(number_to_string.size(), 2);
    EXPECT(number_to_string.find(1) == number_to_string.end());

    number_to_string.remove(3);
    EXPECT_EQ(number_to_string.size(), 1);
    EXPECT(number_to_string.find(3) == number_to_string.end());
    EXPECT(number_to_string.find(2) != number_to_string.end());
}

TEST_CASE(case_insensitive)
{
    HashMap<String, int, CaseInsensitiveStringTraits> casemap;
    EXPECT_EQ(String("nickserv").to_lowercase(), String("NickServ").to_lowercase());
    casemap.set("nickserv", 3);
    casemap.set("NickServ", 3);
    EXPECT_EQ(casemap.size(), 1);
}

TEST_CASE(assert_on_iteration_during_clear)
{
    struct Object {
        ~Object()
        {
            m_map->begin();
        }
        HashMap<int, Object>* m_map;
    };
    HashMap<int, Object> map;
    map.set(0, { &map });
    map.clear();
}

TEST_MAIN(HashMap)
