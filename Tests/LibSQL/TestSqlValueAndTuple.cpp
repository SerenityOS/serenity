/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <unistd.h>

#include <AK/Time.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>
#include <LibSQL/Tuple.h>
#include <LibSQL/Value.h>
#include <LibTest/TestCase.h>

TEST_CASE(null_value)
{
    SQL::Value v(SQL::SQLType::Null);
    EXPECT_EQ(v.type(), SQL::SQLType::Null);
    EXPECT_EQ(v.to_byte_string(), "(null)"sv);
    EXPECT(!v.to_bool().has_value());
    EXPECT(!v.to_int<i32>().has_value());
    EXPECT(!v.to_int<u32>().has_value());
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
        EXPECT_EQ(v.to_byte_string(), "Test"sv);
    }
    {
        SQL::Value v(ByteString("String Test"sv));
        EXPECT_EQ(v.type(), SQL::SQLType::Text);
        EXPECT_EQ(v.to_byte_string(), "String Test"sv);

        v = ByteString("String Test 2"sv);
        EXPECT_EQ(v.type(), SQL::SQLType::Text);
        EXPECT_EQ(v.to_byte_string(), "String Test 2"sv);
    }
    {
        SQL::Value v("const char * Test");
        EXPECT_EQ(v.type(), SQL::SQLType::Text);
        EXPECT_EQ(v.to_byte_string(), "const char * Test"sv);

        v = "const char * Test 2";
        EXPECT_EQ(v.type(), SQL::SQLType::Text);
        EXPECT_EQ(v.to_byte_string(), "const char * Test 2"sv);
    }
}

TEST_CASE(text_value_to_other_types)
{
    {
        SQL::Value v("42");
        EXPECT_EQ(v.type(), SQL::SQLType::Text);

        EXPECT(v.to_int<i32>().has_value());
        EXPECT_EQ(v.to_int<i32>().value(), 42);

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
        EXPECT(!v.to_int<i32>().has_value());
        EXPECT(!v.to_int<u32>().has_value());
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

        EXPECT(v.to_int<i32>().has_value());
        EXPECT_EQ(v.to_int<i32>().value(), 42);
        EXPECT_EQ(v.to_byte_string(), "42"sv);

        EXPECT(v.to_double().has_value());
        EXPECT((v.to_double().value() - 42.0) < NumericLimits<double>().epsilon());

        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());
    }
    {
        SQL::Value v(0);
        EXPECT_EQ(v.type(), SQL::SQLType::Integer);

        EXPECT(v.to_int<i32>().has_value());
        EXPECT_EQ(v.to_int<i32>().value(), 0);

        EXPECT(v.to_bool().has_value());
        EXPECT(!v.to_bool().value());
    }
    {
        SQL::Value v(42);
        EXPECT_EQ(v.type(), SQL::SQLType::Integer);

        EXPECT(v.to_int<i32>().has_value());
        EXPECT_EQ(v.to_int<i32>().value(), 42);
    }
    {
        SQL::Value text("42");
        SQL::Value integer(SQL::SQLType::Integer);
        integer = text;

        EXPECT(integer.to_int<i32>().has_value());
        EXPECT_EQ(integer.to_int<i32>().value(), 42);
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

TEST_CASE(serialize_downsized_int_value)
{
    auto run_test_for_value = [](auto value) {
        using T = decltype(value);
        SQL::Value v(value);

        SQL::Serializer serializer;
        serializer.serialize(v);
        serializer.rewind();

        auto type_flags = serializer.deserialize<u8>();
        auto type_data = type_flags & 0xf0;
        auto type = static_cast<SQL::SQLType>(type_flags & 0x0f);

        EXPECT_NE(type_data, 0);
        EXPECT_EQ(type, SQL::SQLType::Integer);

        auto deserialized = serializer.deserialize<T>();
        EXPECT_EQ(deserialized, value);
    };

    run_test_for_value(NumericLimits<i8>::min());
    run_test_for_value(NumericLimits<i8>::max());

    run_test_for_value(NumericLimits<i16>::min());
    run_test_for_value(NumericLimits<i16>::max());

    run_test_for_value(NumericLimits<i32>::min());
    run_test_for_value(NumericLimits<i32>::max());

    run_test_for_value(NumericLimits<i64>::min());
    run_test_for_value(NumericLimits<i64>::max());

    run_test_for_value(NumericLimits<u8>::min());
    run_test_for_value(NumericLimits<u8>::max());

    run_test_for_value(NumericLimits<u16>::min());
    run_test_for_value(NumericLimits<u16>::max());

    run_test_for_value(NumericLimits<u32>::min());
    run_test_for_value(NumericLimits<u32>::max());

    run_test_for_value(NumericLimits<u64>::min());
    run_test_for_value(NumericLimits<u64>::max());
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

        EXPECT(v.to_int<i32>().has_value());
        EXPECT_EQ(v.to_int<i32>().value(), 3);
        EXPECT_EQ(v.to_byte_string(), "3.14");

        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());

        v = 0.0;
        EXPECT_EQ(v.type(), SQL::SQLType::Float);

        EXPECT(v.to_double().has_value());
        EXPECT(v.to_double().value() < NumericLimits<double>().epsilon());

        EXPECT(v.to_int<i32>().has_value());
        EXPECT_EQ(v.to_int<i32>().value(), 0);
        EXPECT_EQ(v.to_byte_string(), "0"sv);

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

        EXPECT(v.to_int<i32>().has_value());
        EXPECT_EQ(v.to_int<i32>().value(), 4);
    }
    {
        SQL::Value v(-3.14);
        EXPECT_EQ(v.type(), SQL::SQLType::Float);

        EXPECT(v.to_int<i32>().has_value());
        EXPECT_EQ(v.to_int<i32>().value(), -3);
    }
    {
        SQL::Value v(-3.51);
        EXPECT_EQ(v.type(), SQL::SQLType::Float);

        EXPECT(v.to_int<i32>().has_value());
        EXPECT_EQ(v.to_int<i32>().value(), -4);
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

TEST_CASE(to_int)
{
    SQL::Value text("42");
    SQL::Value integer(42);
    EXPECT_EQ(text, integer);
    EXPECT_EQ(integer, text);

    SQL::Value int_64 { static_cast<i64>(123) };
    EXPECT_EQ(int_64.to_int<i8>(), 123);
    EXPECT_EQ(int_64.to_int<i16>(), 123);
    EXPECT_EQ(int_64.to_int<i32>(), 123);
    EXPECT_EQ(int_64.to_int<u8>(), 123u);
    EXPECT_EQ(int_64.to_int<u16>(), 123u);
    EXPECT_EQ(int_64.to_int<u32>(), 123u);
    EXPECT_EQ(int_64.to_int<u64>(), 123u);

    SQL::Value uint_64 { static_cast<i64>(123) };
    EXPECT_EQ(uint_64.to_int<i8>(), 123);
    EXPECT_EQ(uint_64.to_int<i16>(), 123);
    EXPECT_EQ(uint_64.to_int<i32>(), 123);
    EXPECT_EQ(uint_64.to_int<i64>(), 123);
    EXPECT_EQ(uint_64.to_int<u8>(), 123u);
    EXPECT_EQ(uint_64.to_int<u16>(), 123u);
    EXPECT_EQ(uint_64.to_int<u32>(), 123u);
}

TEST_CASE(to_int_failures)
{
    SQL::Value large_int_64 { NumericLimits<i64>::max() };
    EXPECT(!large_int_64.to_int<i8>().has_value());
    EXPECT(!large_int_64.to_int<i16>().has_value());
    EXPECT(!large_int_64.to_int<i32>().has_value());
    EXPECT(!large_int_64.to_int<u8>().has_value());
    EXPECT(!large_int_64.to_int<u16>().has_value());
    EXPECT(!large_int_64.to_int<u32>().has_value());

    SQL::Value large_int_32 { NumericLimits<i32>::max() };
    EXPECT(!large_int_32.to_int<i8>().has_value());
    EXPECT(!large_int_32.to_int<i16>().has_value());
    EXPECT(!large_int_32.to_int<u8>().has_value());
    EXPECT(!large_int_32.to_int<u16>().has_value());

    SQL::Value small_int_64 { NumericLimits<i64>::min() };
    EXPECT(!small_int_64.to_int<i8>().has_value());
    EXPECT(!small_int_64.to_int<i16>().has_value());
    EXPECT(!small_int_64.to_int<i32>().has_value());
    EXPECT(!small_int_64.to_int<u8>().has_value());
    EXPECT(!small_int_64.to_int<u16>().has_value());
    EXPECT(!small_int_64.to_int<u32>().has_value());
    EXPECT(!small_int_64.to_int<u64>().has_value());

    SQL::Value small_int_32 { NumericLimits<i32>::min() };
    EXPECT(!small_int_32.to_int<i8>().has_value());
    EXPECT(!small_int_32.to_int<i16>().has_value());
    EXPECT(!small_int_32.to_int<u8>().has_value());
    EXPECT(!small_int_32.to_int<u16>().has_value());
    EXPECT(!small_int_32.to_int<u32>().has_value());
    EXPECT(!small_int_32.to_int<u64>().has_value());

    SQL::Value large_uint_64 { NumericLimits<u64>::max() };
    EXPECT(!large_uint_64.to_int<i8>().has_value());
    EXPECT(!large_uint_64.to_int<i16>().has_value());
    EXPECT(!large_uint_64.to_int<i32>().has_value());
    EXPECT(!large_uint_64.to_int<i64>().has_value());
    EXPECT(!large_uint_64.to_int<u8>().has_value());
    EXPECT(!large_uint_64.to_int<u16>().has_value());
    EXPECT(!large_uint_64.to_int<u32>().has_value());

    SQL::Value large_uint_32 { NumericLimits<u32>::max() };
    EXPECT(!large_uint_32.to_int<i8>().has_value());
    EXPECT(!large_uint_32.to_int<i16>().has_value());
    EXPECT(!large_uint_32.to_int<u8>().has_value());
    EXPECT(!large_uint_32.to_int<u16>().has_value());
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

        EXPECT(v.to_int<i32>().has_value());
        EXPECT_EQ(v.to_int<i32>().value(), 1);
        EXPECT_EQ(v.to_byte_string(), "true"sv);

        EXPECT(v.to_double().has_value());
        EXPECT((v.to_double().value() - 1.0) < NumericLimits<double>().epsilon());
    }
    {
        SQL::Value v(false);
        EXPECT_EQ(v.type(), SQL::SQLType::Boolean);

        EXPECT(v.to_bool().has_value());
        EXPECT(!v.to_bool().value());

        EXPECT(v.to_int<i32>().has_value());
        EXPECT_EQ(v.to_int<i32>().value(), 0);
        EXPECT_EQ(v.to_byte_string(), "false"sv);

        EXPECT(v.to_double().has_value());
        EXPECT(v.to_double().value() < NumericLimits<double>().epsilon());
    }
    {
        SQL::Value v(true);
        EXPECT_EQ(v.type(), SQL::SQLType::Boolean);

        EXPECT(v.to_bool().has_value());
        EXPECT(v.to_bool().value());

        EXPECT(v.to_int<i32>().has_value());
        EXPECT_EQ(v.to_int<i32>().value(), 1);
        EXPECT_EQ(v.to_byte_string(), "true"sv);

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

TEST_CASE(unix_date_time_value)
{
    auto now = UnixDateTime::now();
    {
        SQL::Value value(now);
        EXPECT_EQ(value.type(), SQL::SQLType::Integer);

        auto result = value.to_unix_date_time();
        VERIFY(result.has_value());
        EXPECT_EQ(result->milliseconds_since_epoch(), now.milliseconds_since_epoch());
    }
    {
        auto now_plus_10s = now + Duration::from_seconds(10);

        SQL::Value value(now_plus_10s);
        EXPECT_EQ(value.type(), SQL::SQLType::Integer);

        auto result = value.to_unix_date_time();
        VERIFY(result.has_value());
        EXPECT_EQ(result->milliseconds_since_epoch(), now_plus_10s.milliseconds_since_epoch());
    }
    {
        auto now_minus_10s = now - Duration::from_seconds(10);

        SQL::Value value(now_minus_10s);
        EXPECT_EQ(value.type(), SQL::SQLType::Integer);

        auto result = value.to_unix_date_time();
        VERIFY(result.has_value());
        EXPECT_EQ(result->milliseconds_since_epoch(), now_minus_10s.milliseconds_since_epoch());
    }
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

TEST_CASE(add)
{
    {
        SQL::Value value1 { 21 };
        SQL::Value value2 { 42 };

        auto result = value1.add(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 63);
    }
    {
        SQL::Value value1 { 21 };
        SQL::Value value2 { static_cast<u8>(42) };

        auto result = value1.add(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 63);
    }
    {
        SQL::Value value1 { static_cast<u8>(21) };
        SQL::Value value2 { 42 };

        auto result = value1.add(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 63);
    }
    {
        SQL::Value value1 { static_cast<double>(21) };
        SQL::Value value2 { 42 };

        auto result = value1.add(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 63);
    }
    {
        SQL::Value value1 { static_cast<double>(21.5) };
        SQL::Value value2 { 42 };

        auto result = value1.add(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Float);
        EXPECT((result.value().to_double().value() - 63.5) < NumericLimits<double>().epsilon());
    }
}

TEST_CASE(add_error)
{
    {
        // Fails to coerce value2 to the signedness of value1.
        SQL::Value value1 { 1 };
        SQL::Value value2 { NumericLimits<u64>::max() };

        auto result = value1.add(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Fails to coerce value2 to the signedness of value1.
        SQL::Value value1 { static_cast<u64>(1) };
        SQL::Value value2 { -1 };

        auto result = value1.add(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // The operation itself would overflow.
        SQL::Value value1 { static_cast<u64>(1) };
        SQL::Value value2 { NumericLimits<u64>::max() };

        auto result = value1.add(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Cannot convert value to a number.
        SQL::Value value1 { 1 };
        SQL::Value value2 { "foo"sv };

        auto result = value1.add(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::NumericOperatorTypeMismatch);
    }
}

TEST_CASE(subtract)
{
    {
        SQL::Value value1 { 21 };
        SQL::Value value2 { 42 };

        auto result = value1.subtract(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), -21);
    }
    {
        SQL::Value value1 { 21 };
        SQL::Value value2 { static_cast<u8>(42) };

        auto result = value1.subtract(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), -21);
    }
    {
        SQL::Value value1 { static_cast<u8>(42) };
        SQL::Value value2 { 21 };

        auto result = value1.subtract(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 21);
    }
    {
        SQL::Value value1 { static_cast<double>(21) };
        SQL::Value value2 { 42 };

        auto result = value1.subtract(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), -21);
    }
    {
        SQL::Value value1 { static_cast<double>(21.5) };
        SQL::Value value2 { 42 };

        auto result = value1.subtract(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Float);
        EXPECT((result.value().to_double().value() - 20.5) < NumericLimits<double>().epsilon());
    }
}

TEST_CASE(subtract_error)
{
    {
        // Fails to coerce value2 to the signedness of value1.
        SQL::Value value1 { 1 };
        SQL::Value value2 { NumericLimits<u64>::max() };

        auto result = value1.subtract(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Fails to coerce value2 to the signedness of value1.
        SQL::Value value1 { static_cast<u64>(1) };
        SQL::Value value2 { -1 };

        auto result = value1.subtract(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // The operation itself would overflow.
        SQL::Value value1 { static_cast<u64>(0) };
        SQL::Value value2 { static_cast<u64>(1) };

        auto result = value1.subtract(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Cannot convert value to a number.
        SQL::Value value1 { 1 };
        SQL::Value value2 { "foo"sv };

        auto result = value1.subtract(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::NumericOperatorTypeMismatch);
    }
}

TEST_CASE(multiply)
{
    {
        SQL::Value value1 { 2 };
        SQL::Value value2 { 21 };

        auto result = value1.multiply(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 42);
    }
    {
        SQL::Value value1 { 2 };
        SQL::Value value2 { static_cast<u8>(21) };

        auto result = value1.multiply(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 42);
    }
    {
        SQL::Value value1 { static_cast<u8>(2) };
        SQL::Value value2 { 21 };

        auto result = value1.multiply(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 42);
    }
    {
        SQL::Value value1 { static_cast<double>(2) };
        SQL::Value value2 { 21 };

        auto result = value1.multiply(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 42);
    }
    {
        SQL::Value value1 { static_cast<double>(2.5) };
        SQL::Value value2 { 21 };

        auto result = value1.multiply(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Float);
        EXPECT((result.value().to_double().value() - 52.5) < NumericLimits<double>().epsilon());
    }
}

TEST_CASE(multiply_error)
{
    {
        // Fails to coerce value2 to the signedness of value1.
        SQL::Value value1 { 1 };
        SQL::Value value2 { NumericLimits<u64>::max() };

        auto result = value1.multiply(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Fails to coerce value2 to the signedness of value1.
        SQL::Value value1 { static_cast<u64>(1) };
        SQL::Value value2 { -1 };

        auto result = value1.multiply(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // The operation itself would overflow.
        SQL::Value value1 { NumericLimits<i64>::max() };
        SQL::Value value2 { 2 };

        auto result = value1.multiply(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Cannot convert value to a number.
        SQL::Value value1 { 1 };
        SQL::Value value2 { "foo"sv };

        auto result = value1.multiply(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::NumericOperatorTypeMismatch);
    }
}

TEST_CASE(divide)
{
    {
        SQL::Value value1 { 42 };
        SQL::Value value2 { -2 };

        auto result = value1.divide(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), -21);
    }
    {
        SQL::Value value1 { 42 };
        SQL::Value value2 { static_cast<u8>(2) };

        auto result = value1.divide(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 21);
    }
    {
        SQL::Value value1 { static_cast<u8>(42) };
        SQL::Value value2 { 2 };

        auto result = value1.divide(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 21);
    }
    {
        SQL::Value value1 { static_cast<double>(42) };
        SQL::Value value2 { 2 };

        auto result = value1.divide(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 21);
    }
    {
        SQL::Value value1 { static_cast<double>(43) };
        SQL::Value value2 { 2 };

        auto result = value1.divide(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Float);
        EXPECT((result.value().to_double().value() - 21.5) < NumericLimits<double>().epsilon());
    }
}

TEST_CASE(divide_error)
{
    {
        // The operation itself would overflow.
        SQL::Value value1 { 1 };
        SQL::Value value2 { 0 };

        auto result = value1.divide(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Cannot convert value to a number.
        SQL::Value value1 { 1 };
        SQL::Value value2 { "foo"sv };

        auto result = value1.divide(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::NumericOperatorTypeMismatch);
    }
}

TEST_CASE(modulo)
{
    {
        SQL::Value value1 { 21 };
        SQL::Value value2 { 2 };

        auto result = value1.modulo(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 1);
    }
    {
        SQL::Value value1 { 21 };
        SQL::Value value2 { static_cast<u8>(2) };

        auto result = value1.modulo(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 1);
    }
    {
        SQL::Value value1 { static_cast<u8>(21) };
        SQL::Value value2 { 2 };

        auto result = value1.modulo(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 1);
    }
    {
        SQL::Value value1 { static_cast<double>(21) };
        SQL::Value value2 { 2 };

        auto result = value1.modulo(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 1);
    }
}

TEST_CASE(modulo_error)
{
    {
        // Fails to coerce value2 to the signedness of value1.
        SQL::Value value1 { 1 };
        SQL::Value value2 { NumericLimits<u64>::max() };

        auto result = value1.modulo(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Fails to coerce value2 to the signedness of value1.
        SQL::Value value1 { static_cast<u64>(1) };
        SQL::Value value2 { -1 };

        auto result = value1.modulo(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // The operation itself would overflow.
        SQL::Value value1 { 21 };
        SQL::Value value2 { 0 };

        auto result = value1.modulo(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Cannot convert value to an integer.
        SQL::Value value1 { 1 };
        SQL::Value value2 { "foo"sv };

        auto result = value1.modulo(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::NumericOperatorTypeMismatch);
    }
    {
        // Cannot convert value to an integer.
        SQL::Value value1 { static_cast<double>(21.5) };
        SQL::Value value2 { 2 };

        auto result = value1.modulo(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::NumericOperatorTypeMismatch);
    }
}

TEST_CASE(shift_left)
{
    {
        SQL::Value value1 { 0b0011'0000 };
        SQL::Value value2 { 2 };

        auto result = value1.shift_left(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 0b1100'0000);
    }
    {
        SQL::Value value1 { 0b0011'0000 };
        SQL::Value value2 { static_cast<u8>(2) };

        auto result = value1.shift_left(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 0b1100'0000);
    }
    {
        SQL::Value value1 { static_cast<u8>(0b0011'0000) };
        SQL::Value value2 { 2 };

        auto result = value1.shift_left(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 0b1100'0000);
    }
    {
        SQL::Value value1 { static_cast<double>(0b0011'0000) };
        SQL::Value value2 { 2 };

        auto result = value1.shift_left(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 0b1100'0000);
    }
}

TEST_CASE(shift_left_error)
{
    {
        // Fails to coerce value2 to the signedness of value1.
        SQL::Value value1 { 1 };
        SQL::Value value2 { NumericLimits<u64>::max() };

        auto result = value1.shift_left(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Fails to coerce value2 to the signedness of value1.
        SQL::Value value1 { static_cast<u64>(1) };
        SQL::Value value2 { -1 };

        auto result = value1.shift_left(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // The operation itself would overflow.
        SQL::Value value1 { 21 };
        SQL::Value value2 { -1 };

        auto result = value1.shift_left(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // The operation itself would overflow.
        SQL::Value value1 { 21 };
        SQL::Value value2 { 64 };

        auto result = value1.shift_left(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Cannot convert value to an integer.
        SQL::Value value1 { 1 };
        SQL::Value value2 { "foo"sv };

        auto result = value1.shift_left(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::NumericOperatorTypeMismatch);
    }
    {
        // Cannot convert value to an integer.
        SQL::Value value1 { static_cast<double>(21.5) };
        SQL::Value value2 { 2 };

        auto result = value1.shift_left(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::NumericOperatorTypeMismatch);
    }
}

TEST_CASE(shift_right)
{
    {
        SQL::Value value1 { 0b0011'0000 };
        SQL::Value value2 { 2 };

        auto result = value1.shift_right(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 0b0000'1100);
    }
    {
        SQL::Value value1 { 0b0011'0000 };
        SQL::Value value2 { static_cast<u8>(2) };

        auto result = value1.shift_right(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 0b0000'1100);
    }
    {
        SQL::Value value1 { static_cast<u8>(0b0011'0000) };
        SQL::Value value2 { 2 };

        auto result = value1.shift_right(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 0b0000'1100);
    }
    {
        SQL::Value value1 { static_cast<double>(0b0011'0000) };
        SQL::Value value2 { 2 };

        auto result = value1.shift_right(value2);
        EXPECT_EQ(result.value().type(), SQL::SQLType::Integer);
        EXPECT_EQ(result.value(), 0b0000'1100);
    }
}

TEST_CASE(shift_right_error)
{
    {
        // Fails to coerce value2 to the signedness of value1.
        SQL::Value value1 { 1 };
        SQL::Value value2 { NumericLimits<u64>::max() };

        auto result = value1.shift_right(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Fails to coerce value2 to the signedness of value1.
        SQL::Value value1 { static_cast<u64>(1) };
        SQL::Value value2 { -1 };

        auto result = value1.shift_right(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // The operation itself would overflow.
        SQL::Value value1 { 21 };
        SQL::Value value2 { -1 };

        auto result = value1.shift_right(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // The operation itself would overflow.
        SQL::Value value1 { 21 };
        SQL::Value value2 { 64 };

        auto result = value1.shift_right(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::IntegerOverflow);
    }
    {
        // Cannot convert value to an integer.
        SQL::Value value1 { 1 };
        SQL::Value value2 { "foo"sv };

        auto result = value1.shift_right(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::NumericOperatorTypeMismatch);
    }
    {
        // Cannot convert value to an integer.
        SQL::Value value1 { static_cast<double>(21.5) };
        SQL::Value value2 { 2 };

        auto result = value1.shift_right(value2);
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::NumericOperatorTypeMismatch);
    }
}
