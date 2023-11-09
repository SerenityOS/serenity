/*
 * Copyright (c) 2021, thislooksfun <tlf@thislooks.fun>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/DeprecatedString.h>
#include <AK/HashTable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Vector.h>

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
    HashTable<DeprecatedString> strings;
    strings.set("One");
    strings.set("Two");
    strings.set("Three");

    EXPECT_EQ(strings.is_empty(), false);
    EXPECT_EQ(strings.size(), 3u);
}

TEST_CASE(range_loop)
{
    HashTable<DeprecatedString> strings;
    EXPECT_EQ(strings.set("One"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.set("Two"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.set("Three"), AK::HashSetResult::InsertedNewEntry);

    int loop_counter = 0;
    for (auto& it : strings) {
        EXPECT_EQ(it.is_empty(), false);
        ++loop_counter;
    }
    EXPECT_EQ(loop_counter, 3);
}

TEST_CASE(range_loop_reverse)
{
    Array strings = { "One"sv, "Two"sv, "Three"sv };
    OrderedHashTable<DeprecatedString> table;
    EXPECT_EQ(table.set(strings[0]), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(table.set(strings[1]), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(table.set(strings[2]), AK::HashSetResult::InsertedNewEntry);

    int loop_counter = 0;
    int index = strings.size() - 1;
    for (auto& it : table.in_reverse()) {
        EXPECT_EQ(it, strings[index]);
        ++loop_counter;
        --index;
    }
    EXPECT_EQ(loop_counter, 3);
}

TEST_CASE(table_remove)
{
    HashTable<DeprecatedString> strings;
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
    HashTable<DeprecatedString, CaseInsensitiveStringTraits> casetable;
    EXPECT_EQ(DeprecatedString("nickserv").to_lowercase(), DeprecatedString("NickServ").to_lowercase());
    EXPECT_EQ(casetable.set("nickserv"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(casetable.set("NickServ"), AK::HashSetResult::ReplacedExistingEntry);
    EXPECT_EQ(casetable.size(), 1u);
}

TEST_CASE(many_strings)
{
    HashTable<DeprecatedString> strings;
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.set(DeprecatedString::number(i)), AK::HashSetResult::InsertedNewEntry);
    }
    EXPECT_EQ(strings.size(), 999u);
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.remove(DeprecatedString::number(i)), true);
    }
    EXPECT_EQ(strings.is_empty(), true);
}

TEST_CASE(many_collisions)
{
    struct StringCollisionTraits : public DefaultTraits<DeprecatedString> {
        static unsigned hash(DeprecatedString const&) { return 0; }
    };

    HashTable<DeprecatedString, StringCollisionTraits> strings;
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.set(DeprecatedString::number(i)), AK::HashSetResult::InsertedNewEntry);
    }

    EXPECT_EQ(strings.set("foo"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.size(), 1000u);

    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.remove(DeprecatedString::number(i)), true);
    }

    EXPECT(strings.find("foo") != strings.end());
}

TEST_CASE(space_reuse)
{
    struct StringCollisionTraits : public DefaultTraits<DeprecatedString> {
        static unsigned hash(DeprecatedString const&) { return 0; }
    };

    HashTable<DeprecatedString, StringCollisionTraits> strings;

    // Add a few items to allow it to do initial resizing.
    EXPECT_EQ(strings.set("0"), AK::HashSetResult::InsertedNewEntry);
    for (int i = 1; i < 5; ++i) {
        EXPECT_EQ(strings.set(DeprecatedString::number(i)), AK::HashSetResult::InsertedNewEntry);
        EXPECT_EQ(strings.remove(DeprecatedString::number(i - 1)), true);
    }

    auto capacity = strings.capacity();

    for (int i = 5; i < 999; ++i) {
        EXPECT_EQ(strings.set(DeprecatedString::number(i)), AK::HashSetResult::InsertedNewEntry);
        EXPECT_EQ(strings.remove(DeprecatedString::number(i - 1)), true);
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

TEST_CASE(reinsertion)
{
    OrderedHashTable<DeprecatedString> map;
    map.set("ytidb::LAST_RESULT_ENTRY_KEY");
    map.set("__sak");
    map.remove("__sak");
    map.set("__sak");
}

TEST_CASE(clear_with_capacity_when_empty)
{
    HashTable<int> map;
    map.clear_with_capacity();
    map.set(0);
    map.set(1);
    VERIFY(map.size() == 2);
}

TEST_CASE(iterator_removal)
{
    HashTable<int> map;
    map.set(0);
    map.set(1);

    auto it = map.begin();
    map.remove(it);
    EXPECT_EQ(it, map.end());
    EXPECT_EQ(map.size(), 1u);
}

TEST_CASE(ordered_insertion_and_deletion)
{
    OrderedHashTable<int> table;
    EXPECT_EQ(table.set(0), HashSetResult::InsertedNewEntry);
    EXPECT_EQ(table.set(1), HashSetResult::InsertedNewEntry);
    EXPECT_EQ(table.set(2), HashSetResult::InsertedNewEntry);
    EXPECT_EQ(table.set(3), HashSetResult::InsertedNewEntry);
    EXPECT_EQ(table.size(), 4u);

    auto expect_table = [](OrderedHashTable<int>& table, Span<int> values) {
        auto index = 0u;
        for (auto it = table.begin(); it != table.end(); ++it, ++index) {
            EXPECT_EQ(*it, values[index]);
            EXPECT(table.contains(values[index]));
        }

        index = table.size() - 1;
        for (auto it = table.rbegin(); it != table.rend(); ++it, --index) {
            EXPECT_EQ(*it, values[index]);
            EXPECT(table.contains(values[index]));
        }
    };

    expect_table(table, Array<int, 4> { 0, 1, 2, 3 });

    EXPECT(table.remove(0));
    EXPECT(table.remove(2));
    EXPECT(!table.remove(4));
    EXPECT_EQ(table.size(), 2u);

    expect_table(table, Array<int, 2> { 1, 3 });
}

TEST_CASE(ordered_deletion_and_reinsertion)
{
    OrderedHashTable<int> table;
    table.set(1);
    table.set(3);
    table.remove(1);
    EXPECT_EQ(table.size(), 1u);

    // By adding 1 again but this time in a different position, we
    // test whether the bucket's neighbors are reset properly.
    table.set(1);
    EXPECT_EQ(table.size(), 2u);

    auto it = table.begin();
    EXPECT_EQ(*it, 3);
    ++it;
    EXPECT_EQ(*it, 1);
    ++it;
    EXPECT_EQ(it, table.end());

    auto rit = table.rbegin();
    EXPECT_EQ(*rit, 1);
    ++rit;
    EXPECT_EQ(*rit, 3);
    ++rit;
    EXPECT_EQ(rit, table.rend());
}

TEST_CASE(ordered_take_last)
{
    OrderedHashTable<int> table;
    table.set(1);
    table.set(2);
    table.set(3);

    EXPECT_EQ(table.take_last(), 3);
    EXPECT_EQ(table.take_last(), 2);
    EXPECT_EQ(table.take_last(), 1);
    EXPECT(table.is_empty());
}

TEST_CASE(ordered_iterator_removal)
{
    OrderedHashTable<int> map;
    map.set(0);
    map.set(1);

    auto it = map.begin();
    map.remove(it);
    EXPECT_EQ(it, map.end());
    EXPECT_EQ(map.size(), 1u);
}

TEST_CASE(ordered_remove_from_head)
{
    OrderedHashTable<int> map;
    map.set(1);
    map.set(2);
    map.set(3);
    map.set(4);
    map.set(5);
    map.set(6);

    EXPECT_EQ(map.size(), 6u);

    auto it = map.begin();
    map.remove(it);
    it = map.begin();
    map.remove(it);
    it = map.begin();
    map.remove(it);
    it = map.begin();
    map.remove(it);
    it = map.begin();
    map.remove(it);
    it = map.begin();
    map.remove(it);

    EXPECT_EQ(map.size(), 0u);
}

TEST_CASE(ordered_infinite_loop_clang_regression)
{
    OrderedHashTable<DeprecatedString> map;
    map.set("");
    map.set("1");
    map.set("_cb");
    map.set("2");
    map.set("3");
    map.set("_cb_svref");
    map.set("_cb_svref_expires");
    map.remove("_cb_svref");
    map.remove("_cb_svref_expires");
    map.set("_cb_svref");

    size_t iterations = 0;
    auto size = map.size();
    for (auto it = map.begin(); it != map.end(); ++it) {
        if (++iterations > size) {
            VERIFY(false);
            break;
        }
    }
}

TEST_CASE(values)
{
    OrderedHashTable<int> table;
    table.set(10);
    table.set(30);
    table.set(20);

    Vector<int> values = table.values();

    EXPECT_EQ(values.size(), table.size());
    EXPECT_EQ(values[0], 10);
    EXPECT_EQ(values[1], 30);
    EXPECT_EQ(values[2], 20);
}
