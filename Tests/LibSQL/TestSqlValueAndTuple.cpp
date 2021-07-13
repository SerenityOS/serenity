/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <unistd.h>

#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>
#include <LibSQL/Tuple.h>
#include <LibSQL/Value.h>
#include <LibTest/TestCase.h>

TEST_CASE(text_value)
{
    SQL::Value v(SQL::SQLType::Text);
    v = "Test";
    VERIFY(v.to_string().value() == "Test");
}

TEST_CASE(text_value_to_int)
{
    SQL::Value v(SQL::SQLType::Text);
    v = "42";
    EXPECT_EQ(v.to_int().value(), 42);
}

TEST_CASE(text_value_to_int_crash)
{
    SQL::Value v(SQL::SQLType::Text);
    v = "Test";
    EXPECT_CRASH("Can't convert 'Test' to integer", [&]() { (void) (int) v; return Test::Crash::Failure::DidNotCrash; });
}

TEST_CASE(serialize_text_value)
{
    SQL::Value v(SQL::SQLType::Text);
    v = "Test";
    VERIFY(v.to_string().value() == "Test");

    ByteBuffer buffer;
    v.serialize(buffer);

    size_t offset = 0;
    SQL::Value v2(SQL::SQLType::Text, buffer, offset);
    VERIFY((String)v2 == "Test");
}

TEST_CASE(integer_value)
{
    SQL::Value v(SQL::SQLType::Integer);
    v = 42;
    VERIFY(v.to_int().value() == 42);
}

TEST_CASE(serialize_int_value)
{
    SQL::Value v(SQL::SQLType::Text);
    v = 42;
    VERIFY(v.to_int().value() == 42);

    ByteBuffer buffer;
    v.serialize(buffer);

    size_t offset = 0;
    SQL::Value v2(SQL::SQLType::Text, buffer, offset);
    VERIFY(v2 == v);
}

TEST_CASE(float_value)
{
    SQL::Value v(SQL::SQLType::Float);
    v = 3.14;
    VERIFY(v.to_double().value() - 3.14 < 0.001);
}

TEST_CASE(assign_text_value_to_int)
{
    SQL::Value text(SQL::SQLType::Text);
    text = "42";
    SQL::Value integer(SQL::SQLType::Integer);
    integer = text;
    EXPECT_EQ(integer.to_int().value(), 42);
}

TEST_CASE(assign_int_to_text_value)
{
    SQL::Value text(SQL::SQLType::Text);
    text = 42;
    EXPECT_EQ((String)text, "42");
}

TEST_CASE(copy_value)
{
    SQL::Value text(SQL::SQLType::Text);
    text = 42;
    SQL::Value copy(text);
    EXPECT_EQ((String)copy, "42");
}

TEST_CASE(compare_text_to_int)
{
    SQL::Value text(SQL::SQLType::Text);
    text = 42;
    SQL::Value integer(SQL::SQLType::Integer);
    integer = 42;
    EXPECT(text == integer);
    EXPECT(integer == text);
}

TEST_CASE(order_text_values)
{
    SQL::Value v1(SQL::SQLType::Text);
    v1 = "Test_A";
    SQL::Value v2(SQL::SQLType::Text);
    v2 = "Test_B";
    EXPECT(v1 <= v2);
    EXPECT(v1 < v2);
    EXPECT(v2 >= v1);
    EXPECT(v2 > v1);
}

TEST_CASE(order_int_values)
{
    SQL::Value v1(SQL::SQLType::Integer);
    v1 = 12;
    SQL::Value v2(SQL::SQLType::Integer);
    v2 = 42;
    EXPECT(v1 <= v2);
    EXPECT(v1 < v2);
    EXPECT(v2 >= v1);
    EXPECT(v2 > v1);
}

TEST_CASE(tuple)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "col2", SQL::SQLType::Integer, SQL::Order::Descending });
    SQL::Tuple tuple(descriptor);

    tuple["col1"] = "Test";
    tuple["col2"] = 42;
    VERIFY(tuple[0] == "Test");
    VERIFY(tuple[1] == 42);
}

TEST_CASE(serialize_tuple)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "col2", SQL::SQLType::Integer, SQL::Order::Descending });
    SQL::Tuple tuple(descriptor);

    tuple["col1"] = "Test";
    tuple["col2"] = 42;

    auto buffer = ByteBuffer();
    tuple.serialize(buffer);
    EXPECT_EQ((String)tuple[0], "Test");
    EXPECT_EQ((int)tuple[1], 42);

    size_t offset = 0;
    SQL::Tuple tuple2(descriptor, buffer, offset);
    VERIFY(tuple2[0] == "Test");
    VERIFY(tuple2[1] == 42);
}

TEST_CASE(copy_tuple)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "col2", SQL::SQLType::Integer, SQL::Order::Descending });
    SQL::Tuple tuple(descriptor);

    tuple["col1"] = "Test";
    tuple["col2"] = 42;

    SQL::Tuple copy;
    copy = tuple;
    VERIFY(tuple == copy);

    SQL::Tuple copy_2(copy);
    VERIFY(tuple == copy_2);
}

TEST_CASE(compare_tuples)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "col2", SQL::SQLType::Integer, SQL::Order::Descending });

    SQL::Tuple tuple1(descriptor);
    tuple1["col1"] = "Test";
    tuple1["col2"] = 42;

    SQL::Tuple tuple2(descriptor);
    tuple2["col1"] = "Test";
    tuple2["col2"] = 12;

    SQL::Tuple tuple3(descriptor);
    tuple3["col1"] = "Text";
    tuple3["col2"] = 12;

    EXPECT(tuple1 <= tuple2);
    EXPECT(tuple1 < tuple2);
    EXPECT(tuple2 >= tuple1);
    EXPECT(tuple2 > tuple1);

    EXPECT(tuple1 <= tuple3);
    EXPECT(tuple1 < tuple3);
    EXPECT(tuple3 >= tuple1);
    EXPECT(tuple3 > tuple1);
}
