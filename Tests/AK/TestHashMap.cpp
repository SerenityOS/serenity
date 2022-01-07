/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>

TEST_CASE(construct)
{
    using IntIntMap = HashMap<int, int>;
    EXPECT(IntIntMap().is_empty());
    EXPECT_EQ(IntIntMap().size(), 0u);
}

TEST_CASE(construct_from_initializer_list)
{
    HashMap<int, String> number_to_string {
        { 1, "One" },
        { 2, "Two" },
        { 3, "Three" },
    };
    EXPECT_EQ(number_to_string.is_empty(), false);
    EXPECT_EQ(number_to_string.size(), 3u);
}

TEST_CASE(populate)
{
    HashMap<int, String> number_to_string;
    number_to_string.set(1, "One");
    number_to_string.set(2, "Two");
    number_to_string.set(3, "Three");

    EXPECT_EQ(number_to_string.is_empty(), false);
    EXPECT_EQ(number_to_string.size(), 3u);
}

TEST_CASE(range_loop)
{
    HashMap<int, String> number_to_string;
    EXPECT_EQ(number_to_string.set(1, "One"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(number_to_string.set(2, "Two"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(number_to_string.set(3, "Three"), AK::HashSetResult::InsertedNewEntry);

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
    EXPECT_EQ(number_to_string.set(1, "One"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(number_to_string.set(2, "Two"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(number_to_string.set(3, "Three"), AK::HashSetResult::InsertedNewEntry);

    EXPECT_EQ(number_to_string.remove(1), true);
    EXPECT_EQ(number_to_string.size(), 2u);
    EXPECT(number_to_string.find(1) == number_to_string.end());

    EXPECT_EQ(number_to_string.remove(3), true);
    EXPECT_EQ(number_to_string.size(), 1u);
    EXPECT(number_to_string.find(3) == number_to_string.end());
    EXPECT(number_to_string.find(2) != number_to_string.end());
}

TEST_CASE(remove_all_matching)
{
    HashMap<int, String> map;

    map.set(1, "One");
    map.set(2, "Two");
    map.set(3, "Three");
    map.set(4, "Four");

    EXPECT_EQ(map.size(), 4u);

    EXPECT_EQ(map.remove_all_matching([&](int key, String const& value) { return key == 1 || value == "Two"; }), true);
    EXPECT_EQ(map.size(), 2u);

    EXPECT_EQ(map.remove_all_matching([&](int, String const&) { return false; }), false);
    EXPECT_EQ(map.size(), 2u);

    EXPECT(map.contains(3));
    EXPECT(map.contains(4));

    EXPECT_EQ(map.remove_all_matching([&](int, String const&) { return true; }), true);
    EXPECT_EQ(map.remove_all_matching([&](int, String const&) { return false; }), false);

    EXPECT(map.is_empty());

    EXPECT_EQ(map.remove_all_matching([&](int, String const&) { return true; }), false);
}

TEST_CASE(case_insensitive)
{
    HashMap<String, int, CaseInsensitiveStringTraits> casemap;
    EXPECT_EQ(String("nickserv").to_lowercase(), String("NickServ").to_lowercase());
    EXPECT_EQ(casemap.set("nickserv", 3), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(casemap.set("NickServ", 3), AK::HashSetResult::ReplacedExistingEntry);
    EXPECT_EQ(casemap.size(), 1u);
}

TEST_CASE(hashmap_of_nonnullownptr_get)
{
    struct Object {
        Object(const String& s)
            : string(s)
        {
        }
        String string;
    };

    HashMap<int, NonnullOwnPtr<Object>> objects;
    objects.set(1, make<Object>("One"));
    objects.set(2, make<Object>("Two"));
    objects.set(3, make<Object>("Three"));

    {
        auto x = objects.get(2);
        EXPECT_EQ(x.has_value(), true);
        EXPECT_EQ(x.value()->string, "Two");
    }

    {
        // Do it again to make sure that peeking into the map above didn't
        // remove the value from the map.
        auto x = objects.get(2);
        EXPECT_EQ(x.has_value(), true);
        EXPECT_EQ(x.value()->string, "Two");
    }

    EXPECT_EQ(objects.size(), 3u);
}

TEST_CASE(many_strings)
{
    HashMap<String, int> strings;
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.set(String::number(i), i), AK::HashSetResult::InsertedNewEntry);
    }
    EXPECT_EQ(strings.size(), 999u);
    for (auto& it : strings) {
        EXPECT_EQ(it.key.to_int().value(), it.value);
    }
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.remove(String::number(i)), true);
    }
    EXPECT_EQ(strings.is_empty(), true);
}

TEST_CASE(basic_remove)
{
    HashMap<int, int> map;
    map.set(1, 10);
    map.set(2, 20);
    map.set(3, 30);

    EXPECT_EQ(map.remove(3), true);
    EXPECT_EQ(map.remove(3), false);
    EXPECT_EQ(map.size(), 2u);

    EXPECT_EQ(map.remove(1), true);
    EXPECT_EQ(map.remove(1), false);
    EXPECT_EQ(map.size(), 1u);

    EXPECT_EQ(map.remove(2), true);
    EXPECT_EQ(map.remove(2), false);
    EXPECT_EQ(map.size(), 0u);
}

TEST_CASE(basic_contains)
{
    HashMap<int, int> map;
    map.set(1, 10);
    map.set(2, 20);
    map.set(3, 30);

    EXPECT_EQ(map.contains(1), true);
    EXPECT_EQ(map.contains(2), true);
    EXPECT_EQ(map.contains(3), true);
    EXPECT_EQ(map.contains(4), false);

    EXPECT_EQ(map.remove(3), true);
    EXPECT_EQ(map.contains(3), false);
    EXPECT_EQ(map.contains(1), true);
    EXPECT_EQ(map.contains(2), true);

    EXPECT_EQ(map.remove(2), true);
    EXPECT_EQ(map.contains(2), false);
    EXPECT_EQ(map.contains(3), false);
    EXPECT_EQ(map.contains(1), true);

    EXPECT_EQ(map.remove(1), true);
    EXPECT_EQ(map.contains(1), false);
}
