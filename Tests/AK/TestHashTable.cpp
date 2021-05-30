/*
 * Copyright (c) 2021, thislooksfun <tlf@thislooks.fun>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/HashTable.h>
#include <AK/String.h>

TEST_CASE(construct)
{
    using IntTable = HashTable<int>;
    EXPECT(IntTable().is_empty());
    EXPECT_EQ(IntTable().size(), 0u);
}

TEST_CASE(basic_move)
{
    HashTable<int> foo;
    foo.set(1);
    EXPECT_EQ(foo.size(), 1u);
    auto bar = move(foo);
    EXPECT_EQ(bar.size(), 1u);
    EXPECT_EQ(foo.size(), 0u);
    foo = move(bar);
    EXPECT_EQ(bar.size(), 0u);
    EXPECT_EQ(foo.size(), 1u);
}

TEST_CASE(move_is_not_swap)
{
    HashTable<int> foo;
    foo.set(1);
    HashTable<int> bar;
    bar.set(2);
    foo = move(bar);
    EXPECT(foo.contains(2));
    EXPECT(!bar.contains(1));
    EXPECT_EQ(bar.size(), 0u);
}

TEST_CASE(populate)
{
    HashTable<String> strings;
    strings.set("One");
    strings.set("Two");
    strings.set("Three");

    EXPECT_EQ(strings.is_empty(), false);
    EXPECT_EQ(strings.size(), 3u);
}

TEST_CASE(range_loop)
{
    HashTable<String> strings;
    EXPECT_EQ(strings.set("One"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.set("Two"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.set("Three"), AK::HashSetResult::InsertedNewEntry);

    int loop_counter = 0;
    for (auto& it : strings) {
        EXPECT_EQ(it.is_null(), false);
        ++loop_counter;
    }
    EXPECT_EQ(loop_counter, 3);
}

TEST_CASE(table_remove)
{
    HashTable<String> strings;
    EXPECT_EQ(strings.set("One"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.set("Two"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.set("Three"), AK::HashSetResult::InsertedNewEntry);

    EXPECT_EQ(strings.remove("One"), true);
    EXPECT_EQ(strings.size(), 2u);
    EXPECT(strings.find("One") == strings.end());

    EXPECT_EQ(strings.remove("Three"), true);
    EXPECT_EQ(strings.size(), 1u);
    EXPECT(strings.find("Three") == strings.end());
    EXPECT(strings.find("Two") != strings.end());
}

TEST_CASE(case_insensitive)
{
    HashTable<String, CaseInsensitiveStringTraits> casetable;
    EXPECT_EQ(String("nickserv").to_lowercase(), String("NickServ").to_lowercase());
    EXPECT_EQ(casetable.set("nickserv"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(casetable.set("NickServ"), AK::HashSetResult::ReplacedExistingEntry);
    EXPECT_EQ(casetable.size(), 1u);
}

TEST_CASE(many_strings)
{
    HashTable<String> strings;
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.set(String::number(i)), AK::HashSetResult::InsertedNewEntry);
    }
    EXPECT_EQ(strings.size(), 999u);
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.remove(String::number(i)), true);
    }
    EXPECT_EQ(strings.is_empty(), true);
}

TEST_CASE(many_collisions)
{
    struct StringCollisionTraits : public GenericTraits<String> {
        static unsigned hash(const String&) { return 0; }
    };

    HashTable<String, StringCollisionTraits> strings;
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.set(String::number(i)), AK::HashSetResult::InsertedNewEntry);
    }

    EXPECT_EQ(strings.set("foo"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.size(), 1000u);

    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.remove(String::number(i)), true);
    }

    // FIXME: Doing this with an "EXPECT_NOT_EQ" would be cleaner.
    EXPECT_EQ(strings.find("foo") == strings.end(), false);
}

TEST_CASE(space_reuse)
{
    struct StringCollisionTraits : public GenericTraits<String> {
        static unsigned hash(const String&) { return 0; }
    };

    HashTable<String, StringCollisionTraits> strings;

    // Add a few items to allow it to do initial resizing.
    EXPECT_EQ(strings.set("0"), AK::HashSetResult::InsertedNewEntry);
    for (int i = 1; i < 5; ++i) {
        EXPECT_EQ(strings.set(String::number(i)), AK::HashSetResult::InsertedNewEntry);
        EXPECT_EQ(strings.remove(String::number(i - 1)), true);
    }

    auto capacity = strings.capacity();

    for (int i = 5; i < 999; ++i) {
        EXPECT_EQ(strings.set(String::number(i)), AK::HashSetResult::InsertedNewEntry);
        EXPECT_EQ(strings.remove(String::number(i - 1)), true);
    }

    EXPECT_EQ(strings.capacity(), capacity);
}

TEST_CASE(basic_remove)
{
    HashTable<int> table;
    table.set(1);
    table.set(2);
    table.set(3);

    EXPECT_EQ(table.remove(3), true);
    EXPECT_EQ(table.remove(3), false);
    EXPECT_EQ(table.size(), 2u);

    EXPECT_EQ(table.remove(1), true);
    EXPECT_EQ(table.remove(1), false);
    EXPECT_EQ(table.size(), 1u);

    EXPECT_EQ(table.remove(2), true);
    EXPECT_EQ(table.remove(2), false);
    EXPECT_EQ(table.size(), 0u);
}

TEST_CASE(basic_contains)
{
    HashTable<int> table;
    table.set(1);
    table.set(2);
    table.set(3);

    EXPECT_EQ(table.contains(1), true);
    EXPECT_EQ(table.contains(2), true);
    EXPECT_EQ(table.contains(3), true);
    EXPECT_EQ(table.contains(4), false);

    EXPECT_EQ(table.remove(3), true);
    EXPECT_EQ(table.contains(3), false);
    EXPECT_EQ(table.contains(1), true);
    EXPECT_EQ(table.contains(2), true);

    EXPECT_EQ(table.remove(2), true);
    EXPECT_EQ(table.contains(2), false);
    EXPECT_EQ(table.contains(3), false);
    EXPECT_EQ(table.contains(1), true);

    EXPECT_EQ(table.remove(1), true);
    EXPECT_EQ(table.contains(1), false);
}
