/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <unistd.h>

#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>
#include <LibSQL/Tuple.h>
#include <LibSQL/Value.h>
#include <LibTest/TestCase.h>

TEST_CASE(null_value)
{
    SQL::Value v(SQL::SQLType::Null);
    EXPECT_EQ(v.type(), SQL::SQLType::Null);
    EXPECT_EQ(v.to_string(), "(null)"sv);
    EXPECT(!v.to_bool().has_value());
    EXPECT(!v.to_int().has_value());
    EXPECT(!v.to_u32().has_value());
    EXPECT(!v.to_double().has_value());
}

TEST_CASE(assign_null)
{
    SQL::Value v("Test");
    EXPECT_EQ(v.type(), SQL::SQLType::Text);
    EXPECT(!v.is_null());

    v = SQL::Value();
    EXPECT_EQ(v.type(), SQL::SQLType::Null);
    EXPECT(v.is_null());
}

TEST_CASE(text_value)
{
    {
        SQL::Value v(SQL::SQLType::Text);
        EXPECT_EQ(v.type(), SQL::SQLType::Text);
        EXPECT(v.is_null());

        v = "Test"sv;
        EXPECT_EQ(v.type(), SQL::SQLType::Text);
        EXPECT_EQ(v.to_string(), "Test"sv);
    }
    {
        SQL::Value v(String("String Test"sv));
        EXPECT_EQ(v.type(), SQL::SQLType::Text);
        EXPECT_EQ(v.to_string(), "String Test"sv);

        v = String("String Test 2"sv);
        EXPECT_EQ(v.type(), SQL::SQLType::Text);
        EXPECT_EQ(v.to_string(), "String Test 2"sv);
    }
    {
        SQL::Value v("const char * Test");
        EXPECT_EQ(v.type(), SQL::SQLType::Text);
        EXPECT_EQ(v.to_string(), "const char * Test"sv);

        v = "const char * Test 2";
        EXPECT_EQ(v.type(), SQL::SQLType::Text);
        EXPECT_EQ(v.to_string(), "const char * Test 2"sv);
    }
}

TEST_CASE(text_value_to_other_types)
{
    {
        SQL::Value v("42");
        EXPECT_EQ(v.type(), SQL::SQLType::Text);

        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 42);

        EXPECT(v.to_double().has_value());
        EXPECT((v.to_double().value() - 42.0) < NumericLimits<double>().epsilon());
    }
    {
        SQL::Value v("true");
        EXPECT_EQ(v.type(), SQL::SQLType::Text);

        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());
    }
    {
        SQL::Value v("false");
        EXPECT_EQ(v.type(), SQL::SQLType::Text);

        EXPECT(v.to_bool().has_value());
        EXPECT(!v.to_bool().value());
    }
    {
        SQL::Value v("foo");
        EXPECT_EQ(v.type(), SQL::SQLType::Text);

        EXPECT(!v.to_bool().has_value());
        EXPECT(!v.to_int().has_value());
        EXPECT(!v.to_u32().has_value());
        EXPECT(!v.to_double().has_value());
    }
    {
        SQL::Value v("3.14");
        EXPECT_EQ(v.type(), SQL::SQLType::Text);

        EXPECT(v.to_double().has_value());
        EXPECT((v.to_double().value() - 3.14) < NumericLimits<double>().epsilon());
    }
}

TEST_CASE(assign_int_to_text_value)
{
    SQL::Value v(SQL::SQLType::Text);
    EXPECT_EQ(v.type(), SQL::SQLType::Text);
    EXPECT(v.is_null());

    v = 42;
    EXPECT_EQ(v.type(), SQL::SQLType::Integer);
    EXPECT_EQ(v, 42);
}

TEST_CASE(serialize_text_value)
{
    SQL::Value v("Test");
    EXPECT_EQ(v.type(), SQL::SQLType::Text);
    EXPECT_EQ(v, "Test"sv);

    SQL::Serializer serializer;
    serializer.serialize<SQL::Value>(v);

    serializer.rewind();
    auto v2 = serializer.deserialize<SQL::Value>();
    EXPECT_EQ(v2.type(), SQL::SQLType::Text);
    EXPECT_EQ(v2, "Test"sv);
    EXPECT_EQ(v2, v);
}

TEST_CASE(integer_value)
{
    {
        SQL::Value v(SQL::SQLType::Integer);
        EXPECT_EQ(v.type(), SQL::SQLType::Integer);
        EXPECT(v.is_null());

        v = 42;
        EXPECT_EQ(v.type(), SQL::SQLType::Integer);

        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 42);
        EXPECT_EQ(v.to_string(), "42"sv);

        EXPECT(v.to_double().has_value());
        EXPECT((v.to_double().value() - 42.0) < NumericLimits<double>().epsilon());

        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());
    }
    {
        SQL::Value v(0);
        EXPECT_EQ(v.type(), SQL::SQLType::Integer);

        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 0);

        EXPECT(v.to_bool().has_value());
        EXPECT(!v.to_bool().value());
    }
    {
        SQL::Value v(42);
        EXPECT_EQ(v.type(), SQL::SQLType::Integer);

        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 42);
    }
    {
        SQL::Value text("42");
        SQL::Value integer(SQL::SQLType::Integer);
        integer = text;

        EXPECT(integer.to_int().has_value());
        EXPECT_EQ(integer.to_int().value(), 42);
    }
}

TEST_CASE(serialize_int_value)
{
    SQL::Value v(42);
    EXPECT_EQ(v.type(), SQL::SQLType::Integer);
    EXPECT_EQ(v, 42);

    SQL::Serializer serializer;
    serializer.serialize<SQL::Value>(v);

    serializer.rewind();
    auto v2 = serializer.deserialize<SQL::Value>();
    EXPECT_EQ(v2.type(), SQL::SQLType::Integer);
    EXPECT_EQ(v2, 42);
    EXPECT_EQ(v2, v);
}

TEST_CASE(float_value)
{
    {
        SQL::Value v(SQL::SQLType::Float);
        EXPECT_EQ(v.type(), SQL::SQLType::Float);
        EXPECT(v.is_null());

        v = 3.14;
        EXPECT_EQ(v.type(), SQL::SQLType::Float);

        EXPECT(v.to_double().has_value());
        EXPECT((v.to_double().value() - 3.14) < NumericLimits<double>().epsilon());

        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 3);
        EXPECT_EQ(v.to_string(), "3.14");

        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());

        v = 0.0;
        EXPECT_EQ(v.type(), SQL::SQLType::Float);

        EXPECT(v.to_double().has_value());
        EXPECT(v.to_double().value() < NumericLimits<double>().epsilon());

        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 0);
        EXPECT_EQ(v.to_string(), "0"sv);

        EXPECT(v.to_bool().has_value());
        EXPECT(!v.to_bool().value());
    }
    {
        SQL::Value v(3.14);
        EXPECT_EQ(v.type(), SQL::SQLType::Float);
        EXPECT((v.to_double().value() - 3.14) < NumericLimits<double>().epsilon());
    }
    {
        SQL::Value v(3.51);
        EXPECT_EQ(v.type(), SQL::SQLType::Float);

        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 4);
    }
    {
        SQL::Value v(-3.14);
        EXPECT_EQ(v.type(), SQL::SQLType::Float);

        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), -3);
    }
    {
        SQL::Value v(-3.51);
        EXPECT_EQ(v.type(), SQL::SQLType::Float);

        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), -4);
    }
}

TEST_CASE(serialize_float_value)
{
    SQL::Value v(3.14);
    EXPECT_EQ(v.type(), SQL::SQLType::Float);
    EXPECT(v.to_double().value() - 3.14 < NumericLimits<double>().epsilon());

    SQL::Serializer serializer;
    serializer.serialize<SQL::Value>(v);

    serializer.rewind();
    auto v2 = serializer.deserialize<SQL::Value>();
    EXPECT_EQ(v2.type(), SQL::SQLType::Float);
    EXPECT((v.to_double().value() - 3.14) < NumericLimits<double>().epsilon());
    EXPECT_EQ(v2, v);
}

TEST_CASE(copy_value)
{
    SQL::Value text("42");
    SQL::Value copy(text);
    EXPECT_EQ(copy, "42"sv);
}

TEST_CASE(compare_text_to_int)
{
    SQL::Value text("42");
    SQL::Value integer(42);
    EXPECT_EQ(text, integer);
    EXPECT_EQ(integer, text);
}

TEST_CASE(bool_value)
{
    {
        SQL::Value v(SQL::SQLType::Boolean);
        EXPECT_EQ(v.type(), SQL::SQLType::Boolean);
        EXPECT(v.is_null());

        v = true;
        EXPECT_EQ(v.type(), SQL::SQLType::Boolean);

        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());

        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 1);
        EXPECT_EQ(v.to_string(), "true"sv);

        EXPECT(v.to_double().has_value());
        EXPECT((v.to_double().value() - 1.0) < NumericLimits<double>().epsilon());
    }
    {
        SQL::Value v(false);
        EXPECT_EQ(v.type(), SQL::SQLType::Boolean);

        EXPECT(v.to_bool().has_value());
        EXPECT(!v.to_bool().value());

        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 0);
        EXPECT_EQ(v.to_string(), "false"sv);

        EXPECT(v.to_double().has_value());
        EXPECT(v.to_double().value() < NumericLimits<double>().epsilon());
    }
    {
        SQL::Value v(true);
        EXPECT_EQ(v.type(), SQL::SQLType::Boolean);

        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());

        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 1);
        EXPECT_EQ(v.to_string(), "true"sv);

        EXPECT(v.to_double().has_value());
        EXPECT((v.to_double().value() - 1.0) < NumericLimits<double>().epsilon());
    }
}

TEST_CASE(serialize_boolean_value)
{
    SQL::Value v(true);
    EXPECT_EQ(v.type(), SQL::SQLType::Boolean);
    EXPECT_EQ(v.to_bool(), true);

    SQL::Serializer serializer;
    serializer.serialize<SQL::Value>(v);

    serializer.rewind();
    auto v2 = serializer.deserialize<SQL::Value>();
    EXPECT_EQ(v2.type(), SQL::SQLType::Boolean);
    EXPECT_EQ(v2.to_bool(), true);
    EXPECT_EQ(v, v2);
}

TEST_CASE(tuple_value)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Descending });
    auto v = MUST(SQL::Value::create_tuple(move(descriptor)));

    Vector<SQL::Value> values;
    values.empend("Test");
    values.empend(42);
    MUST(v.assign_tuple(values));

    auto values2 = v.to_vector();
    EXPECT(values2.has_value());
    EXPECT_EQ(values, values2.value());
}

TEST_CASE(copy_tuple_value)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Descending });
    auto v = MUST(SQL::Value::create_tuple(move(descriptor)));

    Vector<SQL::Value> values;
    values.empend("Test");
    values.empend(42);
    MUST(v.assign_tuple(values));

    auto values2 = v;
    EXPECT_EQ(values2.type(), v.type());
    EXPECT_EQ(v.type(), SQL::SQLType::Tuple);
    EXPECT_EQ(values, values2.to_vector().value());
}

TEST_CASE(tuple_value_wrong_type)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    auto v = MUST(SQL::Value::create_tuple(move(descriptor)));

    Vector<SQL::Value> values;
    values.empend(42);

    auto result = v.assign_tuple(move(values));
    EXPECT(result.is_error());
}

TEST_CASE(tuple_value_too_many_values)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    auto v = MUST(SQL::Value::create_tuple(move(descriptor)));

    Vector<SQL::Value> values;
    values.empend("Test");
    values.empend(42);

    auto result = v.assign_tuple(move(values));
    EXPECT(result.is_error());
}

TEST_CASE(tuple_value_not_enough_values)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Ascending });
    auto v = MUST(SQL::Value::create_tuple(move(descriptor)));

    Vector<SQL::Value> values;
    values.empend("Test");
    MUST(v.assign_tuple(values));

    EXPECT_EQ(v.type(), SQL::SQLType::Tuple);

    auto values_opt = v.to_vector();
    EXPECT(values_opt.has_value());
    EXPECT_EQ(values_opt.value().size(), 2u);

    auto col2 = values_opt.value()[1];
    EXPECT_EQ(col2.type(), SQL::SQLType::Integer);
}

TEST_CASE(serialize_tuple_value)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Descending });
    auto v = MUST(SQL::Value::create_tuple(move(descriptor)));

    Vector<SQL::Value> values;
    values.empend("Test");
    values.empend(42);
    MUST(v.assign_tuple(values));

    SQL::Serializer serializer;
    serializer.serialize<SQL::Value>(v);

    serializer.rewind();
    auto v2 = serializer.deserialize<SQL::Value>();
    EXPECT_EQ(v2.type(), SQL::SQLType::Tuple);
    EXPECT_EQ(v, v2);
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
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Descending });
    SQL::Tuple tuple(descriptor);

    tuple["col1"] = "Test";
    tuple["col2"] = 42;
    EXPECT_EQ(tuple[0], "Test"sv);
    EXPECT_EQ(tuple[1], 42);
}

TEST_CASE(serialize_tuple)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Descending });
    SQL::Tuple tuple(descriptor);

    tuple["col1"] = "Test";
    tuple["col2"] = 42;

    EXPECT_EQ(tuple[0], "Test"sv);
    EXPECT_EQ(tuple[1], 42);

    SQL::Serializer serializer;
    serializer.serialize<SQL::Tuple>(tuple);

    serializer.rewind();
    auto tuple2 = serializer.deserialize<SQL::Tuple>();
    EXPECT_EQ(tuple2[0], "Test"sv);
    EXPECT_EQ(tuple2[1], 42);
}

TEST_CASE(copy_tuple)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Descending });
    SQL::Tuple tuple(descriptor);

    tuple["col1"] = "Test";
    tuple["col2"] = 42;

    SQL::Tuple copy;
    copy = tuple;
    EXPECT_EQ(tuple, copy);

    SQL::Tuple copy_2(copy);
    EXPECT_EQ(tuple, copy_2);
}

TEST_CASE(compare_tuples)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Descending });

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
