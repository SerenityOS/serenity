/*
 * Copyright (c) 2021, thislooksfun <tlf@thislooks.fun>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/HashTable.h>
#include <AK/NonnullOwnPtr.h>
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

TEST_CASE(remove_all_matching)
{
    HashTable<int> ints;

    ints.set(1);
    ints.set(2);
    ints.set(3);
    ints.set(4);

    EXPECT_EQ(ints.size(), 4u);

    EXPECT_EQ(ints.remove_all_matching([&](int value) { return value > 2; }), true);
    EXPECT_EQ(ints.remove_all_matching([&](int) { return false; }), false);

    EXPECT_EQ(ints.size(), 2u);

    EXPECT(ints.contains(1));
    EXPECT(ints.contains(2));

    EXPECT_EQ(ints.remove_all_matching([&](int) { return true; }), true);

    EXPECT(ints.is_empty());

    EXPECT_EQ(ints.remove_all_matching([&](int) { return true; }), false);
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
        static unsigned hash(String const&) { return 0; }
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
        static unsigned hash(String const&) { return 0; }
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

TEST_CASE(capacity_leak)
{
    HashTable<int> table;
    for (size_t i = 0; i < 10000; ++i) {
        table.set(i);
        table.remove(i);
    }
    EXPECT(table.capacity() < 100u);
}

TEST_CASE(non_trivial_type_table)
{
    HashTable<NonnullOwnPtr<int>> table;

    table.set(make<int>(3));
    table.set(make<int>(11));

    for (int i = 0; i < 1'000; ++i) {
        table.set(make<int>(-i));
    }
    for (int i = 0; i < 10'000; ++i) {
        table.set(make<int>(i));
        table.remove(make<int>(i));
    }

    EXPECT_EQ(table.remove_all_matching([&](auto&) { return true; }), true);
    EXPECT(table.is_empty());
    EXPECT_EQ(table.remove_all_matching([&](auto&) { return true; }), false);
}

TEST_CASE(floats)
{
    HashTable<float> table;
    table.set(0);
    table.set(1.0f);
    table.set(2.0f);
    EXPECT_EQ(table.size(), 3u);
    EXPECT(table.contains(0));
    EXPECT(table.contains(1.0f));
    EXPECT(table.contains(2.0f));
}

// FIXME: Enable this test once it doesn't trigger UBSAN.
#if 0
TEST_CASE(doubles)
{
    HashTable<double> table;
    table.set(0);
    table.set(1.0);
    table.set(2.0);
    EXPECT_EQ(table.size(), 3u);
    EXPECT(table.contains(0));
    EXPECT(table.contains(1.0));
    EXPECT(table.contains(2.0));
}
#endif

// Inserting and removing a bunch of elements will "thrash" the table, leading to a lot of "deleted" markers.
BENCHMARK_CASE(benchmark_thrashing)
{
    HashTable<int> table;
    // Ensure that there needs to be some copying when rehashing.
    table.set(3);
    table.set(7);
    table.set(11);
    table.set(13);
    for (int i = 0; i < 10'000; ++i) {
        table.set(-i);
    }
    for (int i = 0; i < 10'000'000; ++i) {
        table.set(i);
        table.remove(i);
    }
}
