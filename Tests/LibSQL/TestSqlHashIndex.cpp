/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/HashIndex.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Tuple.h>
#include <LibSQL/Value.h>
#include <LibTest/TestCase.h>
#include <unistd.h>

constexpr static int keys[] = {
    39,
    87,
    77,
    42,
    98,
    40,
    53,
    8,
    37,
    12,
    90,
    72,
    73,
    11,
    88,
    22,
    10,
    82,
    25,
    61,
    97,
    18,
    60,
    68,
    21,
    3,
    58,
    29,
    13,
    17,
    89,
    81,
    16,
    64,
    5,
    41,
    36,
    91,
    38,
    24,
    32,
    50,
    34,
    94,
    49,
    47,
    1,
    6,
    44,
    76,
};
constexpr static u32 pointers[] = {
    92,
    4,
    50,
    47,
    68,
    73,
    24,
    28,
    50,
    93,
    60,
    36,
    92,
    72,
    53,
    26,
    91,
    84,
    25,
    43,
    88,
    12,
    62,
    35,
    96,
    27,
    96,
    27,
    99,
    30,
    21,
    89,
    54,
    60,
    37,
    68,
    35,
    55,
    80,
    2,
    33,
    26,
    93,
    70,
    45,
    44,
    3,
    66,
    75,
    4,
};

NonnullRefPtr<SQL::HashIndex> setup_hash_index(SQL::Serializer&);
void insert_and_get_to_and_from_hash_index(int);
void insert_into_and_scan_hash_index(int);

NonnullRefPtr<SQL::HashIndex> setup_hash_index(SQL::Serializer& serializer)
{
    NonnullRefPtr<SQL::TupleDescriptor> tuple_descriptor = adopt_ref(*new SQL::TupleDescriptor);
    tuple_descriptor->append({ "schema", "table", "key_value", SQL::SQLType::Integer, SQL::Order::Ascending });
    tuple_descriptor->append({ "schema", "table", "text_value", SQL::SQLType::Text, SQL::Order::Ascending });

    auto directory_block_index = serializer.heap().user_value(0);
    if (!directory_block_index) {
        directory_block_index = serializer.heap().request_new_block_index();
        serializer.heap().set_user_value(0, directory_block_index);
    }
    auto hash_index = SQL::HashIndex::construct(serializer, tuple_descriptor, directory_block_index);
    return hash_index;
}

void insert_and_get_to_and_from_hash_index(int num_keys)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto heap = MUST(SQL::Heap::create("/tmp/test.db"));
        TRY_OR_FAIL(heap->open());
        SQL::Serializer serializer(heap);
        auto hash_index = setup_hash_index(serializer);

        for (auto ix = 0; ix < num_keys; ix++) {
            SQL::Key k(hash_index->descriptor());
            k[0] = keys[ix];
            k[1] = DeprecatedString::formatted("The key value is {} and the pointer is {}", keys[ix], pointers[ix]);
            k.set_block_index(pointers[ix]);
            hash_index->insert(k);
        }
#ifdef LIST_HASH_INDEX
        hash_index->list_hash();
#endif
    }

    {
        auto heap = MUST(SQL::Heap::create("/tmp/test.db"));
        TRY_OR_FAIL(heap->open());
        SQL::Serializer serializer(heap);
        auto hash_index = setup_hash_index(serializer);

        for (auto ix = 0; ix < num_keys; ix++) {
            SQL::Key k(hash_index->descriptor());
            k[0] = keys[ix];
            k[1] = DeprecatedString::formatted("The key value is {} and the pointer is {}", keys[ix], pointers[ix]);
            auto pointer_opt = hash_index->get(k);
            VERIFY(pointer_opt.has_value());
            EXPECT_EQ(pointer_opt.value(), pointers[ix]);
        }
    }
}

TEST_CASE(hash_index_one_key)
{
    insert_and_get_to_and_from_hash_index(1);
}

TEST_CASE(hash_index_four_keys)
{
    insert_and_get_to_and_from_hash_index(4);
}

TEST_CASE(hash_index_five_keys)
{
    insert_and_get_to_and_from_hash_index(5);
}

TEST_CASE(hash_index_10_keys)
{
    insert_and_get_to_and_from_hash_index(10);
}

TEST_CASE(hash_index_13_keys)
{
    insert_and_get_to_and_from_hash_index(13);
}

TEST_CASE(hash_index_20_keys)
{
    insert_and_get_to_and_from_hash_index(20);
}

TEST_CASE(hash_index_25_keys)
{
    insert_and_get_to_and_from_hash_index(25);
}

TEST_CASE(hash_index_30_keys)
{
    insert_and_get_to_and_from_hash_index(30);
}

TEST_CASE(hash_index_35_keys)
{
    insert_and_get_to_and_from_hash_index(35);
}

TEST_CASE(hash_index_40_keys)
{
    insert_and_get_to_and_from_hash_index(40);
}

TEST_CASE(hash_index_45_keys)
{
    insert_and_get_to_and_from_hash_index(45);
}

TEST_CASE(hash_index_50_keys)
{
    insert_and_get_to_and_from_hash_index(50);
}

void insert_into_and_scan_hash_index(int num_keys)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto heap = MUST(SQL::Heap::create("/tmp/test.db"));
        TRY_OR_FAIL(heap->open());
        SQL::Serializer serializer(heap);
        auto hash_index = setup_hash_index(serializer);

        for (auto ix = 0; ix < num_keys; ix++) {
            SQL::Key k(hash_index->descriptor());
            k[0] = keys[ix];
            k[1] = DeprecatedString::formatted("The key value is {} and the pointer is {}", keys[ix], pointers[ix]);
            k.set_block_index(pointers[ix]);
            hash_index->insert(k);
        }
#ifdef LIST_HASH_INDEX
        hash_index->list_hash();
#endif
    }

    {
        auto heap = MUST(SQL::Heap::create("/tmp/test.db"));
        TRY_OR_FAIL(heap->open());
        SQL::Serializer serializer(heap);
        auto hash_index = setup_hash_index(serializer);
        Vector<bool> found;
        for (auto ix = 0; ix < num_keys; ix++) {
            found.append(false);
        }

        int count = 0;
        for (auto iter = hash_index->begin(); !iter.is_end(); iter++, count++) {
            auto key = (*iter);
            auto key_value = key[0].to_int<i32>();
            VERIFY(key_value.has_value());

            for (auto ix = 0; ix < num_keys; ix++) {
                if (keys[ix] == key_value) {
                    EXPECT_EQ(key.block_index(), pointers[ix]);
                    if (found[ix])
                        FAIL(DeprecatedString::formatted("Key {}, index {} already found previously", *key_value, ix));
                    found[ix] = true;
                    break;
                }
            }
        }

#ifdef LIST_HASH_INDEX
        hash_index->list_hash();
#endif
        EXPECT_EQ(count, num_keys);
        for (auto ix = 0; ix < num_keys; ix++) {
            if (!found[ix])
                FAIL(DeprecatedString::formatted("Key {}, index {} not found", keys[ix], ix));
        }
    }
}

TEST_CASE(hash_index_scan_one_key)
{
    insert_into_and_scan_hash_index(1);
}

TEST_CASE(hash_index_scan_four_keys)
{
    insert_into_and_scan_hash_index(4);
}

TEST_CASE(hash_index_scan_five_keys)
{
    insert_into_and_scan_hash_index(5);
}

TEST_CASE(hash_index_scan_10_keys)
{
    insert_into_and_scan_hash_index(10);
}

TEST_CASE(hash_index_scan_15_keys)
{
    insert_into_and_scan_hash_index(15);
}

TEST_CASE(hash_index_scan_20_keys)
{
    insert_into_and_scan_hash_index(20);
}

TEST_CASE(hash_index_scan_30_keys)
{
    insert_into_and_scan_hash_index(30);
}

TEST_CASE(hash_index_scan_40_keys)
{
    insert_into_and_scan_hash_index(40);
}

TEST_CASE(hash_index_scan_50_keys)
{
    insert_into_and_scan_hash_index(50);
}
