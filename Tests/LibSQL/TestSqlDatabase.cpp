/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <unistd.h>

#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibCore/System.h>
#include <LibSQL/BTree.h>
#include <LibSQL/Database.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>
#include <LibSQL/Value.h>
#include <LibTest/TestCase.h>

static NonnullRefPtr<SQL::SchemaDef> setup_schema(SQL::Database& db)
{
    auto schema = MUST(SQL::SchemaDef::create("TestSchema"));
    MUST(db.add_schema(schema));
    return schema;
}

// FIXME: using the return value for SQL::TableDef to insert a row results in a segfault
static NonnullRefPtr<SQL::TableDef> setup_table(SQL::Database& db)
{
    auto schema = setup_schema(db);
    auto table = MUST(SQL::TableDef::create(schema, "TestTable"));
    table->append_column("TextColumn", SQL::SQLType::Text);
    table->append_column("IntColumn", SQL::SQLType::Integer);
    EXPECT_EQ(table->num_columns(), 2u);
    MUST(db.add_table(table));
    return table;
}

static void insert_into_table(SQL::Database& db, int count)
{
    auto table = MUST(db.get_table("TestSchema", "TestTable"));

    for (int ix = 0; ix < count; ix++) {
        SQL::Row row(*table);
        StringBuilder builder;
        builder.appendff("Test{}", ix);

        row["TextColumn"] = builder.to_byte_string();
        row["IntColumn"] = ix;
        TRY_OR_FAIL(db.insert(row));
    }
}

static void verify_table_contents(SQL::Database& db, int expected_count)
{
    auto table = MUST(db.get_table("TestSchema", "TestTable"));

    int sum = 0;
    int count = 0;
    auto rows = TRY_OR_FAIL(db.select_all(*table));
    for (auto& row : rows) {
        StringBuilder builder;
        builder.appendff("Test{}", row["IntColumn"].to_int<i32>().value());
        EXPECT_EQ(row["TextColumn"].to_byte_string(), builder.to_byte_string());
        count++;
        sum += row["IntColumn"].to_int<i32>().value();
    }
    EXPECT_EQ(count, expected_count);
    EXPECT_EQ(sum, (expected_count * (expected_count - 1)) / 2);
}

static void commit(SQL::Database& db)
{
    TRY_OR_FAIL(db.commit());
}

static void insert_and_verify(int count)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto db = MUST(SQL::Database::create("/tmp/test.db"));
        MUST(db->open());
        (void)setup_table(db);
        commit(db);
    }
    {
        auto db = MUST(SQL::Database::create("/tmp/test.db"));
        MUST(db->open());
        insert_into_table(db, count);
        commit(db);
    }
    {
        auto db = MUST(SQL::Database::create("/tmp/test.db"));
        MUST(db->open());
        verify_table_contents(db, count);
    }
}

TEST_CASE(create_heap)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto heap = MUST(SQL::Heap::create("/tmp/test.db"));
    TRY_OR_FAIL(heap->open());
    EXPECT_EQ(heap->version(), SQL::Heap::VERSION);
}

TEST_CASE(create_from_dev_random)
{
    auto heap = MUST(SQL::Heap::create("/dev/random"));
    auto should_be_error = heap->open();
    EXPECT(should_be_error.is_error());
}

TEST_CASE(create_from_unreadable_file)
{
    auto heap = MUST(SQL::Heap::create("/etc/shadow"));
    auto should_be_error = heap->open();
    EXPECT(should_be_error.is_error());
}

TEST_CASE(create_in_non_existing_dir)
{
    auto heap = MUST(SQL::Heap::create("/tmp/bogus/test.db"));
    auto should_be_error = heap->open();
    EXPECT(should_be_error.is_error());
}

TEST_CASE(create_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto db = MUST(SQL::Database::create("/tmp/test.db"));
    MUST(db->open());
    commit(db);
}

TEST_CASE(add_schema_to_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto db = MUST(SQL::Database::create("/tmp/test.db"));
    MUST(db->open());
    (void)setup_schema(db);
    commit(db);
}

TEST_CASE(get_schema_from_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto db = MUST(SQL::Database::create("/tmp/test.db"));
        MUST(db->open());
        (void)setup_schema(db);
        commit(db);
    }
    {
        auto db = MUST(SQL::Database::create("/tmp/test.db"));
        MUST(db->open());
        auto schema = MUST(db->get_schema("TestSchema"));
    }
}

TEST_CASE(add_table_to_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto db = MUST(SQL::Database::create("/tmp/test.db"));
    MUST(db->open());
    (void)setup_table(db);
    commit(db);
}

TEST_CASE(get_table_from_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto db = MUST(SQL::Database::create("/tmp/test.db"));
        MUST(db->open());
        (void)setup_table(db);
        commit(db);
    }
    {
        auto db = MUST(SQL::Database::create("/tmp/test.db"));
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

TEST_CASE(reuse_row_storage)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto db = MUST(SQL::Database::create("/tmp/test.db"));
    MUST(db->open());
    (void)setup_table(db);
    auto table = MUST(db->get_table("TestSchema", "TestTable"));

    // Insert row
    SQL::Row row(*table);
    row["TextColumn"] = "text value";
    row["IntColumn"] = 12345;
    TRY_OR_FAIL(db->insert(row));
    TRY_OR_FAIL(db->commit());
    auto original_size_in_bytes = MUST(db->file_size_in_bytes());

    // Remove row
    TRY_OR_FAIL(db->remove(row));
    TRY_OR_FAIL(db->commit());
    auto size_in_bytes_after_removal = MUST(db->file_size_in_bytes());
    EXPECT(size_in_bytes_after_removal <= original_size_in_bytes);

    // Insert same row again
    TRY_OR_FAIL(db->insert(row));
    TRY_OR_FAIL(db->commit());
    auto size_in_bytes_after_reinsertion = MUST(db->file_size_in_bytes());
    EXPECT(size_in_bytes_after_reinsertion <= original_size_in_bytes);
}
