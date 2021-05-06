/* 
 * This file is part of the SerenityOS distribution (https://github.com/JanDeVisser/SerenityOS).
 * Copyright (c) 2021 Jan de Visser.
 * 
 * This program is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU General Public License as published by  
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

#include <unistd.h>

#include <LibTest/TestCase.h>
#include <LibSQL/BTree.h>
#include <LibSQL/Database.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Key.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Tuple.h>
#include <LibSQL/Value.h>

TEST_CASE(text_value)
{
    SQL::Value v(SQL::Text);
    v = "Test";
    VERIFY((String) v == "Test");
}

TEST_CASE(text_value_to_int)
{
    SQL::Value v(SQL::Text);
    v = "42";
    EXPECT_EQ((int) v, 42);
}

TEST_CASE(text_value_to_int_crash)
{
    SQL::Value v(SQL::Text);
    v = "Test";
    EXPECT_CRASH("Can't convert 'Test' to integer", [&]() { (void) (int) v; return Test::Crash::Failure::DidNotCrash; });
}

TEST_CASE(serialize_text_value)
{
    SQL::Value v(SQL::Text);
    v = "Test";
    VERIFY((String) v == "Test");

    ByteBuffer buffer;
    v.serialize(buffer);

    size_t offset = 0;
    SQL::Value v2(SQL::Text, buffer, offset);
    VERIFY((String) v2 == "Test");
}

TEST_CASE(integer_value)
{
    SQL::Value v(SQL::Integer);
    v = 42;
    VERIFY((int) v == 42);
}

TEST_CASE(serialize_int_value)
{
    SQL::Value v(SQL::Text);
    v = 42;
    VERIFY((int) v == 42);

    ByteBuffer buffer;
    v.serialize(buffer);

    size_t offset = 0;
    SQL::Value v2(SQL::Text, buffer, offset);
    VERIFY(v2 == v);
}

TEST_CASE(float_value)
{
    SQL::Value v(SQL::Float);
    v = 3.14;
    VERIFY((double) v - 3.14 < 0.001);
}

TEST_CASE(assign_text_value_to_int)
{
    SQL::Value text(SQL::Text);
    text = "42";
    SQL::Value integer(SQL::Integer);
    integer = text;
    EXPECT_EQ((int) integer, 42);
}

TEST_CASE(assign_int_to_text_value)
{
    SQL::Value text(SQL::Text);
    text = 42;
    EXPECT_EQ((String) text, "42");
}

TEST_CASE(copy_value)
{
    SQL::Value text(SQL::Text);
    text = 42;
    SQL::Value copy(text);
    EXPECT_EQ((String) copy, "42");
}

TEST_CASE(compare_text_to_int)
{
    SQL::Value text(SQL::Text);
    text = 42;
    SQL::Value integer(SQL::Integer);
    integer = 42;
    EXPECT(text == integer);
    EXPECT(integer == text);
}

TEST_CASE(order_text_values)
{
    SQL::Value v1(SQL::Text);
    v1 = "Test_A";
    SQL::Value v2(SQL::Text);
    v2 = "Test_B";
    EXPECT(v1 <= v2);
    EXPECT(v1 < v2);
    EXPECT(v2 >= v1);
    EXPECT(v2 > v1);
}

TEST_CASE(order_int_values)
{
    SQL::Value v1(SQL::Integer);
    v1 = 12;
    SQL::Value v2(SQL::Integer);
    v2 = 42;
    EXPECT(v1 <= v2);
    EXPECT(v1 < v2);
    EXPECT(v2 >= v1);
    EXPECT(v2 > v1);
}

TEST_CASE(key)
{
    auto index_def = SQL::IndexDef::construct("test", false, 0);
    index_def->append_column("col1", SQL::Text, SQL::Ascending);
    index_def->append_column("col2", SQL::Integer, SQL::Descending);
    SQL::Key key(*index_def);

    key["col1"] = "Test";
    key["col2"] = 42;
    VERIFY(key[0] == "Test");
    VERIFY(key[1] == 42);
}

TEST_CASE(serialize_key)
{
    auto index_def = SQL::IndexDef::construct("test", false, 0);
    index_def->append_column("col1", SQL::Text, SQL::Ascending);
    index_def->append_column("col2", SQL::Integer, SQL::Descending);
    SQL::Key key(*index_def);

    key["col1"] = "Test";
    key["col2"] = 42;

    auto buffer = ByteBuffer();
    key.serialize(buffer);
    EXPECT_EQ((String) key[0], "Test");
    EXPECT_EQ((int) key[1], 42);

    size_t offset = 0;
    SQL::Key key2(*index_def, buffer, offset);
    VERIFY(key2[0] == "Test");
    VERIFY(key2[1] == 42);
}

TEST_CASE(copy_key)
{
    auto index_def = SQL::IndexDef::construct("test", false, 0);
    index_def->append_column("col1", SQL::Text, SQL::Ascending);
    index_def->append_column("col2", SQL::Integer, SQL::Descending);
    SQL::Key key(*index_def);

    key["col1"] = "Test";
    key["col2"] = 42;

    SQL::Key copy;
    copy = key;
    VERIFY(key == copy);

    SQL::Key copy_2(copy);
    VERIFY(key == copy_2);
}

TEST_CASE(compare_keys)
{
    auto index_def = SQL::IndexDef::construct("test", false, 0);
    index_def->append_column("col1", SQL::Text, SQL::Ascending);
    index_def->append_column("col2", SQL::Integer, SQL::Ascending);

    SQL::Key key1(*index_def);
    key1["col1"] = "Test";
    key1["col2"] = 12;

    SQL::Key key2(*index_def);
    key2["col1"] = "Test";
    key2["col2"] = 42;

    SQL::Key key3(*index_def);
    key3["col1"] = "Text";
    key3["col2"] = 12;

    EXPECT(key1 <= key2);
    EXPECT(key1 < key2);
    EXPECT(key2 >= key1);
    EXPECT(key2 > key1);

    EXPECT(key1 <= key3);
    EXPECT(key1 < key3);
    EXPECT(key3 >= key1);
    EXPECT(key3 > key1);
}



TEST_CASE(create_heap) {
    unlink("test.db");
    auto heap = SQL::Heap::construct("test.db");
    EXPECT_EQ(heap->version(), 0x00000001u);
}

constexpr static int keys[] = { 39, 87, 77, 42, 98, 40, 53, 8, 37, 12, 90, 72, 73, 11, 88, 22, 10, 82, 25, 61, 97, 18, 60, 68, 21, 3, 58, 29, 13, 17, 89, 81, 16, 64, 5, 41, 36, 91, 38, 24, 32, 50, 34, 94, 49, 47, 1, 6, 44, 76, };
constexpr static u32 pointers[] = { 92, 4, 50, 47, 68, 73, 24, 28, 50, 93, 60, 36, 92, 72, 53, 26, 91, 84, 25, 43, 88, 12, 62, 35, 96, 27, 96, 27, 99, 30, 21, 89, 54, 60, 37, 68, 35, 55, 80, 2, 33, 26, 93, 70, 45, 44, 3, 66, 75, 4, };

NonnullRefPtr<SQL::BTree> setup_btree(SQL::Heap& heap);
void insert_and_get_to_and_from_btree(int num_keys);
void insert_into_and_scan_btree(int num_keys);


NonnullRefPtr<SQL::BTree> setup_btree(SQL::Heap& heap)
{
    auto index_def = SQL::IndexDef::construct("test", true, 0);
    index_def->append_column("key_value", SQL::SQLType::Integer, SQL::SortOrder::Ascending);

    auto root_pointer = heap.user_value(0);
    if (!root_pointer) {
        root_pointer = heap.new_record_pointer();
        heap.set_user_value(0, root_pointer);
    }
    auto btree = SQL::BTree::construct(heap, index_def, root_pointer);
    btree->on_new_root = [&]() {
        heap.set_user_value(0, btree->root());
    };
    return btree;
}

void insert_and_get_to_and_from_btree(int num_keys)
{
    unlink("test.db");
    {
        auto heap = SQL::Heap::construct("test.db");
        auto btree = setup_btree(heap);

        for (auto ix = 0; ix < num_keys; ix++) {
            SQL::Key k(btree->index_def());
            k[0] = keys[ix];
            k.pointer(pointers[ix]);
            btree->insert(k);
        }
#ifdef LIST_TREE
        btree->list_tree();
#endif
    }

    {
        auto heap = SQL::Heap::construct("test.db");
        auto btree = setup_btree(heap);

        for (auto ix = 0; ix < num_keys; ix++) {
            SQL::Key k(btree->index_def());
            k[0] = keys[ix];
            auto pointer_opt = btree->get(k);
            EXPECT(pointer_opt.has_value());
            EXPECT_EQ(pointer_opt.value(), pointers[ix]);
        }
    }
}

void insert_into_and_scan_btree(int num_keys)
{
    unlink("test.db");
    {
        auto heap = SQL::Heap::construct("test.db");
        auto btree = setup_btree(heap);

        for (auto ix = 0; ix < num_keys; ix++) {
            SQL::Key k(btree->index_def());
            k[0] = keys[ix];
            k.pointer(pointers[ix]);
            btree->insert(k);
        }
#ifdef LIST_TREE
        btree->list_tree();
#endif
    }

    {
        auto heap = SQL::Heap::construct("test.db");
        auto btree = setup_btree(heap);

        int count = 0;
        SQL::Key prev;
        for (auto iter = btree->begin(); !iter.is_end(); iter++, count++) {
            auto key = (*iter);
            dbgln((String) key);
            if (prev.length()) {
                EXPECT(prev < key);
            }
            auto key_value = (int) key[0];
            for (auto ix = 0; ix < num_keys; ix++) {
                if (keys[ix] == key_value) {
                    EXPECT_EQ(key.pointer(), pointers[ix]);
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

TEST_CASE(create_database) {
    unlink("test.db");
    auto db = SQL::Database::construct("test.db");
    db->commit();
}

TEST_CASE(add_table_to_btree) {
    unlink("test.db");
    auto db = SQL::Database::construct("test.db");
    auto table = SQL::TableDef::construct("Test");

    table->append_column("TextColumn", SQL::Text);
    table->append_column("IntColumn", SQL::Integer);
    db->add_table(table);
    db->commit();
}

TEST_CASE(get_table_from_btree) {
    auto db = SQL::Database::construct("test.db");
    auto table = db->get_table("Test");
    EXPECT(table);
    EXPECT_EQ(table->name(), "Test");
    EXPECT_EQ(table->num_columns(), 2u);
    db->commit();
}

TEST_CASE(insert_into_table) {
    auto db = SQL::Database::construct("test.db");
    auto table = db->get_table("Test");
    EXPECT(table);

    SQL::Tuple tuple(*table);
    tuple["TextColumn"] = "Test123";
    tuple["IntColumn"] = 42;
    EXPECT(db->insert(tuple));
    db->commit();
}

TEST_CASE(select_from_table) {
    auto db = SQL::Database::construct("test.db");
    auto table = db->get_table("Test");
    EXPECT(table);

    int count = 0;
    for (auto &tuple : db->select_all(*table)) {
        EXPECT_EQ(tuple["TextColumn"], "Test123");
        EXPECT_EQ(tuple["IntColumn"], "42");
        count++;
    }
    EXPECT_EQ(count, 1);
}

TEST_CASE(insert_more_into_table) {
    auto db = SQL::Database::construct("test.db");
    auto table = db->get_table("Test");
    EXPECT(table);

    for (int count = 0; count < 10; count++) {
        SQL::Tuple tuple(*table);
        StringBuilder builder;
        builder.appendff("Test{}", count);

        tuple["TextColumn"] = builder.build();
        tuple["IntColumn"] = count;
        EXPECT(db->insert(tuple));
    }
    db->commit();
}

TEST_CASE(select_more_from_table) {
    auto db = SQL::Database::construct("test.db");
    auto table = db->get_table("Test");
    EXPECT(table);

    auto results = db->select_all(*table);
    EXPECT_EQ(results.size(), 11u);
}
