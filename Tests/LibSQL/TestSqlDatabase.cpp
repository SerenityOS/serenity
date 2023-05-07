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
    MUST(db.add_schema(schema));
    return schema;
}

NonnullRefPtr<SQL::TableDef> setup_table(SQL::Database& db)
{
    auto schema = setup_schema(db);
    auto table = SQL::TableDef::construct(schema, "TestTable");
    table->append_column("TextColumn", SQL::SQLType::Text);
    table->append_column("IntColumn", SQL::SQLType::Integer);
    EXPECT_EQ(table->num_columns(), 2u);
    MUST(db.add_table(table));
    return table;
}

void insert_into_table(SQL::Database& db, int count)
{
    auto table = MUST(db.get_table("TestSchema", "TestTable"));

    for (int ix = 0; ix < count; ix++) {
        SQL::Row row(*table);
        StringBuilder builder;
        builder.appendff("Test{}", ix);

        row["TextColumn"] = builder.to_deprecated_string();
        row["IntColumn"] = ix;
        TRY_OR_FAIL(db.insert(row));
    }
}

void verify_table_contents(SQL::Database& db, int expected_count)
{
    auto table = MUST(db.get_table("TestSchema", "TestTable"));

    int sum = 0;
    int count = 0;
    auto rows = TRY_OR_FAIL(db.select_all(*table));
    for (auto& row : rows) {
        StringBuilder builder;
        builder.appendff("Test{}", row["IntColumn"].to_int<i32>().value());
        EXPECT_EQ(row["TextColumn"].to_deprecated_string(), builder.to_deprecated_string());
        count++;
        sum += row["IntColumn"].to_int<i32>().value();
    }
    EXPECT_EQ(count, expected_count);
    EXPECT_EQ(sum, (expected_count * (expected_count - 1)) / 2);
}

void commit(SQL::Database& db)
{
    TRY_OR_FAIL(db.commit());
}

void insert_and_verify(int count)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        MUST(db->open());
        (void)setup_table(db);
        commit(db);
    }
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        MUST(db->open());
        insert_into_table(db, count);
        commit(db);
    }
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        MUST(db->open());
        verify_table_contents(db, count);
    }
}

TEST_CASE(create_heap)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto heap = SQL::Heap::construct("/tmp/test.db");
    TRY_OR_FAIL(heap->open());
    EXPECT_EQ(heap->version(), SQL::Heap::VERSION);
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
    MUST(db->open());
    commit(db);
}

TEST_CASE(add_schema_to_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto db = SQL::Database::construct("/tmp/test.db");
    MUST(db->open());
    (void)setup_schema(db);
    commit(db);
}

TEST_CASE(get_schema_from_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        MUST(db->open());
        (void)setup_schema(db);
        commit(db);
    }
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        MUST(db->open());
        auto schema = MUST(db->get_schema("TestSchema"));
    }
}

TEST_CASE(add_table_to_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto db = SQL::Database::construct("/tmp/test.db");
    MUST(db->open());
    (void)setup_table(db);
    commit(db);
}

TEST_CASE(get_table_from_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        MUST(db->open());
        (void)setup_table(db);
        commit(db);
    }
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        MUST(db->open());

        auto table = MUST(db->get_table("TestSchema", "TestTable"));
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
