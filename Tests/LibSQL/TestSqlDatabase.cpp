/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <unistd.h>

#include <AK/ScopeGuard.h>
#include <LibSQL/BTree.h>
#include <LibSQL/Database.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>
#include <LibSQL/Value.h>
#include <LibTest/TestCase.h>

NonnullRefPtr<SQL::SchemaDef> setup_schema(SQL::Database&);
NonnullRefPtr<SQL::TableDef> setup_table(SQL::Database&);
void insert_into_table(SQL::Database&, int);
void verify_table_contents(SQL::Database&, int);
void insert_and_verify(int);
void commit(SQL::Database&);

NonnullRefPtr<SQL::SchemaDef> setup_schema(SQL::Database& db)
{
    auto schema = SQL::SchemaDef::construct("TestSchema");
    auto maybe_error = db.add_schema(schema);
    EXPECT(!maybe_error.is_error());
    return schema;
}

NonnullRefPtr<SQL::TableDef> setup_table(SQL::Database& db)
{
    auto schema = setup_schema(db);
    auto table = SQL::TableDef::construct(schema, "TestTable");
    table->append_column("TextColumn", SQL::SQLType::Text);
    table->append_column("IntColumn", SQL::SQLType::Integer);
    EXPECT_EQ(table->num_columns(), 2u);
    auto maybe_error = db.add_table(table);
    EXPECT(!maybe_error.is_error());
    return table;
}

void insert_into_table(SQL::Database& db, int count)
{
    auto table_or_error = db.get_table("TestSchema", "TestTable");
    EXPECT(!table_or_error.is_error());
    auto table = table_or_error.value();
    EXPECT(table);

    for (int ix = 0; ix < count; ix++) {
        SQL::Row row(*table);
        StringBuilder builder;
        builder.appendff("Test{}", ix);

        row["TextColumn"] = builder.build();
        row["IntColumn"] = ix;
        auto maybe_error = db.insert(row);
        EXPECT(!maybe_error.is_error());
    }
}

void verify_table_contents(SQL::Database& db, int expected_count)
{
    auto table_or_error = db.get_table("TestSchema", "TestTable");
    EXPECT(!table_or_error.is_error());
    auto table = table_or_error.value();
    EXPECT(table);

    int sum = 0;
    int count = 0;
    auto rows_or_error = db.select_all(*table);
    EXPECT(!rows_or_error.is_error());
    for (auto& row : rows_or_error.value()) {
        StringBuilder builder;
        builder.appendff("Test{}", row["IntColumn"].to_int().value());
        EXPECT_EQ(row["TextColumn"].to_string(), builder.build());
        count++;
        sum += row["IntColumn"].to_int().value();
    }
    EXPECT_EQ(count, expected_count);
    EXPECT_EQ(sum, (expected_count * (expected_count - 1)) / 2);
}

void commit(SQL::Database& db)
{
    auto maybe_error = db.commit();
    EXPECT(!maybe_error.is_error());
}

void insert_and_verify(int count)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        EXPECT(!db->open().is_error());
        (void)setup_table(db);
        commit(db);
    }
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        EXPECT(!db->open().is_error());
        insert_into_table(db, count);
        commit(db);
    }
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        EXPECT(!db->open().is_error());
        verify_table_contents(db, count);
    }
}

TEST_CASE(create_heap)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto heap = SQL::Heap::construct("/tmp/test.db");
    EXPECT(!heap->open().is_error());
    EXPECT_EQ(heap->version(), 0x00000001u);
}

TEST_CASE(create_from_dev_random)
{
    auto heap = SQL::Heap::construct("/dev/random");
    auto should_be_error = heap->open();
    EXPECT(should_be_error.is_error());
}

TEST_CASE(create_from_unreadable_file)
{
    auto heap = SQL::Heap::construct("/etc/shadow");
    auto should_be_error = heap->open();
    EXPECT(should_be_error.is_error());
}

TEST_CASE(create_in_non_existing_dir)
{
    auto heap = SQL::Heap::construct("/tmp/bogus/test.db");
    auto should_be_error = heap->open();
    EXPECT(should_be_error.is_error());
}

TEST_CASE(create_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto db = SQL::Database::construct("/tmp/test.db");
    auto should_not_be_error = db->open();
    EXPECT(!should_not_be_error.is_error());
    commit(db);
}

TEST_CASE(add_schema_to_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto db = SQL::Database::construct("/tmp/test.db");
    EXPECT(!db->open().is_error());
    (void)setup_schema(db);
    commit(db);
}

TEST_CASE(get_schema_from_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        EXPECT(!db->open().is_error());
        (void)setup_schema(db);
        commit(db);
    }
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        EXPECT(!db->open().is_error());
        auto schema_or_error = db->get_schema("TestSchema");
        EXPECT(!schema_or_error.is_error());
        EXPECT(schema_or_error.value());
    }
}

TEST_CASE(add_table_to_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto db = SQL::Database::construct("/tmp/test.db");
    EXPECT(!db->open().is_error());
    (void)setup_table(db);
    commit(db);
}

TEST_CASE(get_table_from_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        EXPECT(!db->open().is_error());
        (void)setup_table(db);
        commit(db);
    }
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        EXPECT(!db->open().is_error());
        auto table_or_error = db->get_table("TestSchema", "TestTable");
        EXPECT(!table_or_error.is_error());
        auto table = table_or_error.value();
        EXPECT(table);
        EXPECT_EQ(table->name(), "TestTable");
        EXPECT_EQ(table->num_columns(), 2u);
    }
}

TEST_CASE(insert_one_into_and_select_from_table)
{
    insert_and_verify(1);
}

TEST_CASE(insert_two_into_table)
{
    insert_and_verify(2);
}

TEST_CASE(insert_10_into_table)
{
    insert_and_verify(10);
}

TEST_CASE(insert_100_into_table)
{
    insert_and_verify(100);
}
