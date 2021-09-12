/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <unistd.h>

#include <AK/ScopeGuard.h>
#include <LibSQL/Database.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>
#include <LibTest/TestCase.h>

NonnullRefPtr<SQL::SchemaDef> setup_schema(SQL::Database&);
NonnullRefPtr<SQL::TableDef> setup_table(SQL::Database&);
void insert_into_table(SQL::Database&, int);
void verify_table_contents(SQL::Database&, int);
void insert_and_verify(int);

NonnullRefPtr<SQL::SchemaDef> setup_schema(SQL::Database& db)
{
    auto schema = SQL::SchemaDef::construct("TestSchema");
    db.add_schema(schema);
    return schema;
}

NonnullRefPtr<SQL::TableDef> setup_table(SQL::Database& db)
{
    auto schema = setup_schema(db);
    auto table = SQL::TableDef::construct(schema, "TestTable");
    db.add_table(table);
    table->append_column("TextColumn", SQL::SQLType::Text);
    table->append_column("IntColumn", SQL::SQLType::Integer);
    EXPECT_EQ(table->num_columns(), 2u);
    db.add_table(table);
    return table;
}

void insert_into_table(SQL::Database& db, int count)
{
    auto table = db.get_table("TestSchema", "TestTable");
    EXPECT(table);

    for (int ix = 0; ix < count; ix++) {
        SQL::Row row(*table);
        StringBuilder builder;
        builder.appendff("Test{}", ix);

        row["TextColumn"] = builder.build();
        row["IntColumn"] = ix;
        EXPECT(db.insert(row));
    }
}

void verify_table_contents(SQL::Database& db, int expected_count)
{
    auto table = db.get_table("TestSchema", "TestTable");
    EXPECT(table);

    int sum = 0;
    int count = 0;
    for (auto& row : db.select_all(*table)) {
        StringBuilder builder;
        builder.appendff("Test{}", row["IntColumn"].to_int().value());
        EXPECT_EQ(row["TextColumn"].to_string(), builder.build());
        count++;
        sum += row["IntColumn"].to_int().value();
    }
    EXPECT_EQ(count, expected_count);
    EXPECT_EQ(sum, (expected_count * (expected_count - 1)) / 2);
}

void insert_and_verify(int count)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        setup_table(db);
        db->commit();
    }
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        insert_into_table(db, count);
        db->commit();
    }
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        verify_table_contents(db, count);
    }
}

TEST_CASE(create_heap)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto heap = SQL::Heap::construct("/tmp/test.db");
    EXPECT_EQ(heap->version(), 0x00000001u);
}

TEST_CASE(create_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto db = SQL::Database::construct("/tmp/test.db");
    db->commit();
}

TEST_CASE(add_schema_to_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto db = SQL::Database::construct("/tmp/test.db");
    setup_schema(db);
    db->commit();
}

TEST_CASE(get_schema_from_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        setup_schema(db);
        db->commit();
    }
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        auto schema = db->get_schema("TestSchema");
        EXPECT(schema);
    }
}

TEST_CASE(add_table_to_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    auto db = SQL::Database::construct("/tmp/test.db");
    setup_table(db);
    db->commit();
}

TEST_CASE(get_table_from_database)
{
    ScopeGuard guard([]() { unlink("/tmp/test.db"); });
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        setup_table(db);
        db->commit();
    }
    {
        auto db = SQL::Database::construct("/tmp/test.db");
        auto table = db->get_table("TestSchema", "TestTable");
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
