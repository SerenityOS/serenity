/*
 * Copyright (c) 2021, thislooksfun <tlf@thislooks.fun>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/TestSuite.h>

#include <AK/HashTable.h>
#include <AK/String.h>

TEST_CASE(construct)
{
    using IntTable = HashTable<int>;
    EXPECT(IntTable().is_empty());
    EXPECT_EQ(IntTable().size(), 0u);
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

TEST_MAIN(HashTable)
