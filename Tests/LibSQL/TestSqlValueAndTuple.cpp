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

TEST_CASE(null_value)
{
    SQL::Value v(SQL::SQLType::Null);
    EXPECT(v.type() == SQL::SQLType::Null);
    EXPECT(v.is_null());
    v = "Test";
    EXPECT(v.is_null());
    EXPECT(v.to_string() == "(null)");
}

TEST_CASE(text_value)
{
    {
        SQL::Value v(SQL::SQLType::Text);
        EXPECT(v.is_null());
        v = "Test";
        EXPECT(!v.is_null());
        EXPECT(v.to_string() == "Test");
    }
    {
        SQL::Value v(SQL::SQLType::Text, String("String Test"));
        EXPECT(!v.is_null());
        EXPECT(v.to_string() == "String Test");
    }
    {
        SQL::Value v(SQL::SQLType::Text, "const char * Test");
        EXPECT(!v.is_null());
        EXPECT_EQ(v.to_string(), "const char * Test");
    }
    {
        SQL::Value v(String("String Test"));
        EXPECT(v.type() == SQL::SQLType::Text);
        EXPECT(!v.is_null());
        EXPECT(v.to_string() == "String Test");
    }
    {
        SQL::Value v(SQL::SQLType::Text, SQL::Value(42));
        EXPECT(v.type() == SQL::SQLType::Text);
        EXPECT(!v.is_null());
        EXPECT(v.to_string() == "42");
    }
}

TEST_CASE(assign_null)
{
    SQL::Value v("Test");
    EXPECT(!v.is_null());
    v = SQL::Value::null();
    EXPECT(v.is_null());
}

TEST_CASE(text_value_to_other_types)
{
    {
        SQL::Value v(SQL::SQLType::Text, "42");
        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 42);
        EXPECT(v.to_double().has_value());
        EXPECT(v.to_double().value() - 42.0 < NumericLimits<double>().epsilon());
    }
    {
        SQL::Value v("true");
        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());
    }
    {
        SQL::Value v("false");
        EXPECT(v.to_bool().has_value());
        EXPECT(!v.to_bool().value());
    }
}

TEST_CASE(text_value_to_int_crash)
{
    SQL::Value v(SQL::SQLType::Text, "Not a valid integer");
    EXPECT_CRASH("Can't convert 'Not a valid integer' to integer", [&]() { (void) (int) v; return Test::Crash::Failure::DidNotCrash; });
}

TEST_CASE(serialize_text_value)
{
    SQL::Value v("Test");
    EXPECT(v.to_string() == "Test");

    SQL::Serializer serializer;
    serializer.serialize<SQL::Value>(v);

    serializer.rewind();
    auto v2 = serializer.deserialize<SQL::Value>();
    EXPECT((String)v2 == "Test");
}

TEST_CASE(integer_value)
{
    {
        SQL::Value v(SQL::SQLType::Integer);
        EXPECT(v.is_null());
        v = 42;
        EXPECT(!v.is_null());
        EXPECT(v.to_int().value() == 42);
        EXPECT(v.to_string() == "42");
        EXPECT(v.to_double().has_value());
        EXPECT(v.to_double().value() - 42.0 < NumericLimits<double>().epsilon());
        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());
    }
    {
        SQL::Value v(0);
        EXPECT(!v.is_null());
        EXPECT(v.to_int().value() == 0);
        EXPECT(v.to_bool().has_value());
        EXPECT(!v.to_bool().value());
    }
    {
        SQL::Value v(SQL::SQLType::Integer, "42");
        EXPECT_EQ(v.to_int().value(), 42);
    }
    {
        SQL::Value v(SQL::SQLType::Integer, SQL::Value("42"));
        EXPECT_EQ(v.to_int().value(), 42);
    }
    {
        SQL::Value text("42");
        SQL::Value integer(SQL::SQLType::Integer);
        integer = text;
        EXPECT_EQ(integer.to_int().value(), 42);
    }
}

TEST_CASE(serialize_int_value)
{
    SQL::Value v(42);
    EXPECT_EQ(v.type(), SQL::SQLType::Integer);
    EXPECT_EQ(v.to_int().value(), 42);

    SQL::Serializer serializer;
    serializer.serialize<SQL::Value>(v);

    serializer.rewind();
    auto v2 = serializer.deserialize<SQL::Value>();
    EXPECT(!v2.is_null());
    EXPECT_EQ(v2.type(), SQL::SQLType::Integer);
    EXPECT_EQ(v2.to_int().value(), 42);
    EXPECT(v2 == v);
}

TEST_CASE(float_value)
{
    {
        SQL::Value v(SQL::SQLType::Float);
        EXPECT(v.is_null());
        v = 3.14;
        EXPECT(!v.is_null());
        EXPECT(v.to_double().has_value());
        EXPECT(v.to_double().value() - 3.14 < NumericLimits<double>().epsilon());
        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 3);
        EXPECT_EQ(v.to_string(), "3.14");
        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());

        v = 0.0;
        EXPECT(!v.is_null());
        EXPECT(v.to_double().has_value());
        EXPECT(v.to_double().value() < NumericLimits<double>().epsilon());
        EXPECT(v.to_int().has_value());
        EXPECT_EQ(v.to_int().value(), 0);
        EXPECT_EQ(v.to_string(), "0");
        EXPECT(v.to_bool().has_value());
        EXPECT(!v.to_bool().value());
    }
    {
        SQL::Value v(3.14);
        EXPECT(!v.is_null());
        EXPECT(v.to_double().value() - 3.14 < NumericLimits<double>().epsilon());
    }
    {
        SQL::Value v(3.51);
        EXPECT(!v.is_null());
        EXPECT_EQ(v.to_int().value(), 4);
    }
    {
        SQL::Value v(-3.14);
        EXPECT_EQ(v.to_int().value(), -3);
    }
    {
        SQL::Value v(-3.51);
        EXPECT_EQ(v.to_int().value(), -4);
    }
    {
        SQL::Value v(SQL::SQLType::Float, "3.14");
        EXPECT(v.to_double().value() - 3.14 < NumericLimits<double>().epsilon());
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
    EXPECT(!v2.is_null());
    EXPECT_EQ(v2.type(), SQL::SQLType::Float);
    EXPECT(v.to_double().value() - 3.14 < NumericLimits<double>().epsilon());
}

TEST_CASE(assign_int_to_text_value)
{
    SQL::Value text(SQL::SQLType::Text);
    text = 42;
    EXPECT_EQ((String)text, "42");
}

TEST_CASE(copy_value)
{
    SQL::Value text(SQL::SQLType::Text, 42);
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

TEST_CASE(bool_value)
{
    {
        SQL::Value v(SQL::SQLType::Boolean);
        EXPECT(v.is_null());
        v = true;
        EXPECT(!v.is_null());
        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());
        EXPECT_EQ(v.to_int().value(), 1);
        EXPECT_EQ(v.to_string(), "true");
        EXPECT(!v.to_double().has_value());
    }
    {
        SQL::Value v(SQL::SQLType::Boolean, false);
        EXPECT(!v.is_null());
        EXPECT(v.to_bool().has_value());
        EXPECT(!v.to_bool().value());
        EXPECT_EQ(v.to_int().value(), 0);
        EXPECT_EQ(v.to_string(), "false");
        EXPECT(!v.to_double().has_value());
    }
    {
        SQL::Value v(true);
        EXPECT_EQ(v.type(), SQL::SQLType::Boolean);
        EXPECT(!v.is_null());
        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());
    }
}

TEST_CASE(serialize_boolean_value)
{
    SQL::Value v(true);
    EXPECT_EQ(v.type(), SQL::SQLType::Boolean);
    EXPECT(bool(v));

    SQL::Serializer serializer;
    serializer.serialize<SQL::Value>(v);

    serializer.rewind();
    auto v2 = serializer.deserialize<SQL::Value>();
    EXPECT(!v2.is_null());
    EXPECT_EQ(v2.type(), SQL::SQLType::Boolean);
    EXPECT(bool(v2));
    EXPECT_EQ(v, v2);
}

TEST_CASE(tuple_value)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Descending });

    auto v = SQL::Value::create_tuple(descriptor);
    Vector<SQL::Value> values;
    values.append(SQL::Value("Test"));
    values.append(SQL::Value(42));
    v = values;

    auto values2 = v.to_vector();
    EXPECT(values2.has_value());
    EXPECT_EQ(values, values2.value());
}

TEST_CASE(copy_tuple_value)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Descending });

    auto v = SQL::Value::create_tuple(descriptor);
    Vector<SQL::Value> values;
    values.append(SQL::Value("Test"));
    values.append(SQL::Value(42));
    v = values;

    auto values2 = v;
    EXPECT(values2.type() == v.type());
    EXPECT(!values2.is_null());
    EXPECT_EQ(values, values2.to_vector().value());
}

TEST_CASE(tuple_value_wrong_type)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });

    auto v = SQL::Value::create_tuple(descriptor);
    Vector<SQL::Value> values;
    values.append(SQL::Value(42));
    v = values;
    EXPECT(v.is_null());
}

TEST_CASE(tuple_value_too_many_values)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });

    auto v = SQL::Value::create_tuple(descriptor);
    Vector<SQL::Value> values;
    values.append(SQL::Value("Test"));
    values.append(SQL::Value(42));
    v = values;
    EXPECT(v.is_null());
}

TEST_CASE(tuple_value_not_enough_values)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Ascending });

    auto v = SQL::Value::create_tuple(descriptor);
    Vector<SQL::Value> values;
    values.append(SQL::Value("Test"));
    v = values;
    EXPECT(!v.is_null());
    auto values_opt = v.to_vector();
    EXPECT(values_opt.has_value());
    EXPECT_EQ(values_opt.value().size(), 2u);
    auto col2 = values_opt.value()[1];
    EXPECT_EQ(col2.type(), SQL::SQLType::Integer);
    EXPECT(col2.is_null());
}

TEST_CASE(serialize_tuple_value)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Descending });

    auto v = SQL::Value::create_tuple(descriptor);
    Vector<SQL::Value> values;
    values.append(SQL::Value("Test"));
    values.append(SQL::Value(42));
    v = values;

    SQL::Serializer serializer;
    serializer.serialize<SQL::Value>(v);

    serializer.rewind();
    auto v2 = serializer.deserialize<SQL::Value>();
    EXPECT(!v2.is_null());
    EXPECT_EQ(v2.type(), SQL::SQLType::Tuple);
    EXPECT_EQ(v, v2);
}

TEST_CASE(array_value)
{
    auto v = SQL::Value::create_array(SQL::SQLType::Text, 3);
    Vector<SQL::Value> values;
    values.append(SQL::Value("Test 1"));
    values.append(SQL::Value("Test 2"));
    v = values;

    auto values2 = v.to_vector();
    EXPECT(values2.has_value());
    EXPECT_EQ(values, values2.value());
}

TEST_CASE(array_value_wrong_type)
{
    auto v = SQL::Value::create_array(SQL::SQLType::Text, 2);
    Vector<SQL::Value> values;
    values.append(SQL::Value("Test 1"));
    values.append(SQL::Value(42));
    v = values;
    EXPECT(v.is_null());
}

TEST_CASE(array_value_too_many_values)
{
    auto v = SQL::Value::create_array(SQL::SQLType::Text, 2);
    Vector<SQL::Value> values;
    values.append(SQL::Value("Test 1"));
    values.append(SQL::Value("Test 2"));
    values.append(SQL::Value("Test 3"));
    v = values;
    EXPECT(v.is_null());
}

TEST_CASE(copy_array_value)
{
    auto v = SQL::Value::create_array(SQL::SQLType::Text, 3);
    Vector<SQL::Value> values;
    values.append(SQL::Value("Test 1"));
    values.append(SQL::Value("Test 2"));
    v = values;

    auto values2 = v;
    EXPECT(values2.type() == v.type());
    EXPECT(!values2.is_null());
    EXPECT_EQ(values, values2.to_vector().value());
}

TEST_CASE(serialize_array_value)
{
    auto v = SQL::Value::create_array(SQL::SQLType::Text, 3);
    Vector<SQL::Value> values;
    values.append(SQL::Value("Test 1"));
    values.append(SQL::Value("Test 2"));
    v = values;

    SQL::Serializer serializer;
    serializer.serialize<SQL::Value>(v);

    serializer.rewind();
    auto v2 = serializer.deserialize<SQL::Value>();
    EXPECT(!v2.is_null());
    EXPECT_EQ(v2.type(), SQL::SQLType::Array);
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
    EXPECT(tuple[0] == "Test");
    EXPECT(tuple[1] == 42);
}

TEST_CASE(serialize_tuple)
{
    NonnullRefPtr<SQL::TupleDescriptor> descriptor = adopt_ref(*new SQL::TupleDescriptor);
    descriptor->append({ "schema", "table", "col1", SQL::SQLType::Text, SQL::Order::Ascending });
    descriptor->append({ "schema", "table", "col2", SQL::SQLType::Integer, SQL::Order::Descending });
    SQL::Tuple tuple(descriptor);

    tuple["col1"] = "Test";
    tuple["col2"] = 42;

    EXPECT_EQ((String)tuple[0], "Test");
    EXPECT_EQ((int)tuple[1], 42);

    SQL::Serializer serializer;
    serializer.serialize<SQL::Tuple>(tuple);

    serializer.rewind();
    auto tuple2 = serializer.deserialize<SQL::Tuple>();
    EXPECT(tuple2[0] == "Test");
    EXPECT(tuple2[1] == 42);
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
    EXPECT(tuple == copy);

    SQL::Tuple copy_2(copy);
    EXPECT(tuple == copy_2);
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
