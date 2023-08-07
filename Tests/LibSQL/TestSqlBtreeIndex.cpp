/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <unistd.h>

#include <AK/ScopeGuard.h>
#include <LibSQL/BTree.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Key.h>
#include <LibSQL/Meta.h>
#include <LibSQL/TupleDescriptor.h>
#include <LibSQL/Value.h>
#include <LibTest/TestCase.h>

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

NonnullRefPtr<SQL::BTree> setup_btree(SQL::Serializer&);
void insert_and_get_to_and_from_btree(int);
void insert_into_and_scan_btree(int);

NonnullRefPtr<SQL::BTree> setup_btree(SQL::Serializer& serializer)
{
    NonnullRefPtr<SQL::TupleDescriptor> tuple_descriptor = adopt_ref(*new SQL::TupleDescriptor);
    tuple_descriptor->append({ "schema", "table", "key_value", SQL::SQLType::Integer, SQL::Order::Ascending });

    auto root_pointer = serializer.heap().user_value(0);
    if (!root_pointer) {
        root_pointer = serializer.heap().request_new_block_index();
        serializer.heap().set_user_value(0, root_pointer);
    }
    auto btree = MUST(SQL::BTree::create(serializer, tuple_descriptor, true, root_pointer));
    btree->on_new_root = [&]() {
        serializer.heap().set_user_value(0, btree->root());
    };
    return btree;
}

void insert_and_get_to_and_from_btree(int num_keys)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto heap = MUST(SQL::Heap::create("/tmp/test.db"));
        TRY_OR_FAIL(heap->open());
        SQL::Serializer serializer(heap);
        auto btree = setup_btree(serializer);

        for (auto ix = 0; ix < num_keys; ix++) {
            SQL::Key k(btree->descriptor());
            k[0] = keys[ix];
            k.set_block_index(pointers[ix]);
            btree->insert(k);
        }
#ifdef LIST_TREE
        btree->list_tree();
#endif
    }

    {
        auto heap = MUST(SQL::Heap::create("/tmp/test.db"));
        TRY_OR_FAIL(heap->open());
        SQL::Serializer serializer(heap);
        auto btree = setup_btree(serializer);

        for (auto ix = 0; ix < num_keys; ix++) {
            SQL::Key k(btree->descriptor());
            k[0] = keys[ix];
            auto pointer_opt = btree->get(k);
            VERIFY(pointer_opt.has_value());
            EXPECT_EQ(pointer_opt.value(), pointers[ix]);
        }
    }
}

void insert_into_and_scan_btree(int num_keys)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto heap = MUST(SQL::Heap::create("/tmp/test.db"));
        TRY_OR_FAIL(heap->open());
        SQL::Serializer serializer(heap);
        auto btree = setup_btree(serializer);

        for (auto ix = 0; ix < num_keys; ix++) {
            SQL::Key k(btree->descriptor());
            k[0] = keys[ix];
            k.set_block_index(pointers[ix]);
            btree->insert(k);
        }

#ifdef LIST_TREE
        btree->list_tree();
#endif
    }

    {
        auto heap = MUST(SQL::Heap::create("/tmp/test.db"));
        TRY_OR_FAIL(heap->open());
        SQL::Serializer serializer(heap);
        auto btree = setup_btree(serializer);

        int count = 0;
        SQL::Tuple prev;
        for (auto iter = btree->begin(); !iter.is_end(); iter++, count++) {
            auto key = (*iter);
            if (prev.size())
                EXPECT(prev < key);
            auto key_value = key[0].to_int<i32>();
            for (auto ix = 0; ix < num_keys; ix++) {
                if (keys[ix] == key_value) {
                    EXPECT_EQ(key.block_index(), pointers[ix]);
                    break;
                }
            }
            prev = key;
        }
        EXPECT_EQ(count, num_keys);
    }
}

TEST_CASE(btree_one_key)
{
    insert_and_get_to_and_from_btree(1);
}

TEST_CASE(btree_four_keys)
{
    insert_and_get_to_and_from_btree(4);
}

TEST_CASE(btree_five_keys)
{
    insert_and_get_to_and_from_btree(5);
}

TEST_CASE(btree_10_keys)
{
    insert_and_get_to_and_from_btree(10);
}

TEST_CASE(btree_13_keys)
{
    insert_and_get_to_and_from_btree(13);
}

TEST_CASE(btree_20_keys)
{
    insert_and_get_to_and_from_btree(20);
}

TEST_CASE(btree_25_keys)
{
    insert_and_get_to_and_from_btree(25);
}

TEST_CASE(btree_30_keys)
{
    insert_and_get_to_and_from_btree(30);
}

TEST_CASE(btree_35_keys)
{
    insert_and_get_to_and_from_btree(35);
}

TEST_CASE(btree_40_keys)
{
    insert_and_get_to_and_from_btree(40);
}

TEST_CASE(btree_45_keys)
{
    insert_and_get_to_and_from_btree(45);
}

TEST_CASE(btree_50_keys)
{
    insert_and_get_to_and_from_btree(50);
}

TEST_CASE(btree_scan_one_key)
{
    insert_into_and_scan_btree(1);
}

TEST_CASE(btree_scan_four_keys)
{
    insert_into_and_scan_btree(4);
}

TEST_CASE(btree_scan_five_keys)
{
    insert_into_and_scan_btree(5);
}

TEST_CASE(btree_scan_10_keys)
{
    insert_into_and_scan_btree(10);
}

TEST_CASE(btree_scan_15_keys)
{
    insert_into_and_scan_btree(15);
}

TEST_CASE(btree_scan_30_keys)
{
    insert_into_and_scan_btree(15);
}

TEST_CASE(btree_scan_50_keys)
{
    insert_into_and_scan_btree(50);
}
