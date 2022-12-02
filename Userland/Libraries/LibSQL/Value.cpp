/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>
#include <LibSQL/AST/AST.h>
#include <LibSQL/Serializer.h>
#include <LibSQL/TupleDescriptor.h>
#include <LibSQL/Value.h>
#include <math.h>
#include <string.h>

namespace SQL {

Value::Value(SQLType type)
    : m_type(type)
{
}

Value::Value(DeprecatedString value)
    : m_type(SQLType::Text)
    , m_value(move(value))
{
}

Value::Value(int value)
    : m_type(SQLType::Integer)
    , m_value(value)
{
}

Value::Value(u32 value)
    : m_type(SQLType::Integer)
    , m_value(static_cast<int>(value)) // FIXME: Handle signed overflow.
{
}

Value::Value(double value)
    : m_type(SQLType::Float)
    , m_value(value)
{
}

Value::Value(NonnullRefPtr<TupleDescriptor> descriptor, Vector<Value> values)
    : m_type(SQLType::Tuple)
    , m_value(TupleValue { move(descriptor), move(values) })
{
}

Value::Value(Value const& other)
    : m_type(other.m_type)
    , m_value(other.m_value)
{
}

Value::Value(Value&& other)
    : m_type(other.m_type)
    , m_value(move(other.m_value))
{
}

Value::~Value() = default;

ResultOr<Value> Value::create_tuple(NonnullRefPtr<TupleDescriptor> descriptor)
{
    Vector<Value> values;
    TRY(values.try_resize(descriptor->size()));

    for (size_t i = 0; i < descriptor->size(); ++i)
        values[i].m_type = descriptor->at(i).type;

    return Value { move(descriptor), move(values) };
}

ResultOr<Value> Value::create_tuple(Vector<Value> values)
{
    auto descriptor = TRY(infer_tuple_descriptor(values));
    return Value { move(descriptor), move(values) };
}

SQLType Value::type() const
{
    return m_type;
}

StringView Value::type_name() const
{
    switch (type()) {
#undef __ENUMERATE_SQL_TYPE
#define __ENUMERATE_SQL_TYPE(name, cardinal, type, impl, size) \
    case SQLType::type:                                        \
        return name##sv;
        ENUMERATE_SQL_TYPES(__ENUMERATE_SQL_TYPE)
#undef __ENUMERATE_SQL_TYPE
    default:
        VERIFY_NOT_REACHED();
    }
}

bool Value::is_type_compatible_with(SQLType other_type) const
{
    switch (type()) {
    case SQLType::Null:
        return false;
    case SQLType::Integer:
    case SQLType::Float:
        return other_type == SQLType::Integer || other_type == SQLType::Float;
    default:
        break;
    }

    return type() == other_type;
}

bool Value::is_null() const
{
    return !m_value.has_value();
}

DeprecatedString Value::to_deprecated_string() const
{
    if (is_null())
        return "(null)"sv;

    return m_value->visit(
        [](DeprecatedString const& value) -> DeprecatedString { return value; },
        [](int value) -> DeprecatedString { return DeprecatedString::number(value); },
        [](double value) -> DeprecatedString { return DeprecatedString::number(value); },
        [](bool value) -> DeprecatedString { return value ? "true"sv : "false"sv; },
        [](TupleValue const& value) -> DeprecatedString {
            StringBuilder builder;

            builder.append('(');
            builder.join(',', value.values);
            builder.append(')');

            return builder.build();
        });
}

Optional<int> Value::to_int() const
{
    if (is_null())
        return {};

    return m_value->visit(
        [](DeprecatedString const& value) -> Optional<int> { return value.to_int(); },
        [](int value) -> Optional<int> { return value; },
        [](double value) -> Optional<int> {
            if (value > static_cast<double>(NumericLimits<int>::max()))
                return {};
            if (value < static_cast<double>(NumericLimits<int>::min()))
                return {};
            return static_cast<int>(round(value));
        },
        [](bool value) -> Optional<int> { return static_cast<int>(value); },
        [](TupleValue const&) -> Optional<int> { return {}; });
}

Optional<u32> Value::to_u32() const
{
    // FIXME: Handle negative values.
    if (auto result = to_int(); result.has_value())
        return static_cast<u32>(result.value());
    return {};
}

Optional<double> Value::to_double() const
{
    if (is_null())
        return {};

    return m_value->visit(
        [](DeprecatedString const& value) -> Optional<double> {
            char* end = nullptr;
            double result = strtod(value.characters(), &end);

            if (end == value.characters())
                return {};
            return result;
        },
        [](int value) -> Optional<double> { return static_cast<double>(value); },
        [](double value) -> Optional<double> { return value; },
        [](bool value) -> Optional<double> { return static_cast<double>(value); },
        [](TupleValue const&) -> Optional<double> { return {}; });
}

Optional<bool> Value::to_bool() const
{
    if (is_null())
        return {};

    return m_value->visit(
        [](DeprecatedString const& value) -> Optional<bool> {
            if (value.equals_ignoring_case("true"sv) || value.equals_ignoring_case("t"sv))
                return true;
            if (value.equals_ignoring_case("false"sv) || value.equals_ignoring_case("f"sv))
                return false;
            return {};
        },
        [](int value) -> Optional<bool> { return static_cast<bool>(value); },
        [](double value) -> Optional<bool> { return fabs(value) > NumericLimits<double>::epsilon(); },
        [](bool value) -> Optional<bool> { return value; },
        [](TupleValue const& value) -> Optional<bool> {
            for (auto const& element : value.values) {
                auto as_bool = element.to_bool();
                if (!as_bool.has_value())
                    return {};
                if (!as_bool.value())
                    return false;
            }

            return true;
        });
}

Optional<Vector<Value>> Value::to_vector() const
{
    if (is_null() || (type() != SQLType::Tuple))
        return {};

    auto const& tuple = m_value->get<TupleValue>();
    return tuple.values;
}

Value& Value::operator=(Value value)
{
    m_type = value.m_type;
    m_value = move(value.m_value);
    return *this;
}

Value& Value::operator=(DeprecatedString value)
{
    m_type = SQLType::Text;
    m_value = move(value);
    return *this;
}

Value& Value::operator=(int value)
{
    m_type = SQLType::Integer;
    m_value = value;
    return *this;
}

Value& Value::operator=(u32 value)
{
    m_type = SQLType::Integer;
    m_value = static_cast<int>(value); // FIXME: Handle signed overflow.
    return *this;
}

Value& Value::operator=(double value)
{
    m_type = SQLType::Float;
    m_value = value;
    return *this;
}

ResultOr<void> Value::assign_tuple(NonnullRefPtr<TupleDescriptor> descriptor)
{
    Vector<Value> values;
    TRY(values.try_resize(descriptor->size()));

    for (size_t i = 0; i < descriptor->size(); ++i)
        values[i].m_type = descriptor->at(i).type;

    m_type = SQLType::Tuple;
    m_value = TupleValue { move(descriptor), move(values) };

    return {};
}

ResultOr<void> Value::assign_tuple(Vector<Value> values)
{
    if (is_null() || (type() != SQLType::Tuple)) {
        auto descriptor = TRY(infer_tuple_descriptor(values));

        m_type = SQLType::Tuple;
        m_value = TupleValue { move(descriptor), move(values) };

        return {};
    }

    auto& tuple = m_value->get<TupleValue>();

    if (values.size() > tuple.descriptor->size())
        return Result { SQLCommand::Unknown, SQLErrorCode::InvalidNumberOfValues };

    for (size_t i = 0; i < values.size(); ++i) {
        if (values[i].type() != tuple.descriptor->at(i).type)
            return Result { SQLCommand::Unknown, SQLErrorCode::InvalidType, SQLType_name(values[i].type()) };
    }

    if (values.size() < tuple.descriptor->size()) {
        size_t original_size = values.size();
        MUST(values.try_resize(tuple.descriptor->size()));

        for (size_t i = original_size; i < values.size(); ++i)
            values[i].m_type = tuple.descriptor->at(i).type;
    }

    m_value = TupleValue { move(tuple.descriptor), move(values) };
    return {};
}

size_t Value::length() const
{
    if (is_null())
        return 0;

    // FIXME: This seems to be more of an encoded byte size rather than a length.
    return m_value->visit(
        [](DeprecatedString const& value) -> size_t { return sizeof(u32) + value.length(); },
        [](int value) -> size_t { return sizeof(value); },
        [](double value) -> size_t { return sizeof(value); },
        [](bool value) -> size_t { return sizeof(value); },
        [](TupleValue const& value) -> size_t {
            auto size = value.descriptor->length() + sizeof(u32);

            for (auto const& element : value.values)
                size += element.length();

            return size;
        });
}

u32 Value::hash() const
{
    if (is_null())
        return 0;

    return m_value->visit(
        [](DeprecatedString const& value) -> u32 { return value.hash(); },
        [](int value) -> u32 { return int_hash(value); },
        [](double) -> u32 { VERIFY_NOT_REACHED(); },
        [](bool value) -> u32 { return int_hash(value); },
        [](TupleValue const& value) -> u32 {
            u32 hash = 0;

            for (auto const& element : value.values) {
                if (hash == 0)
                    hash = element.hash();
                else
                    hash = pair_int_hash(hash, element.hash());
            }

            return hash;
        });
}

int Value::compare(Value const& other) const
{
    if (is_null())
        return -1;
    if (other.is_null())
        return 1;

    return m_value->visit(
        [&](DeprecatedString const& value) -> int { return value.view().compare(other.to_deprecated_string()); },
        [&](int value) -> int {
            auto casted = other.to_int();
            if (!casted.has_value())
                return 1;

            if (value == *casted)
                return 0;
            return value < *casted ? -1 : 1;
        },
        [&](double value) -> int {
            auto casted = other.to_double();
            if (!casted.has_value())
                return 1;

            auto diff = value - *casted;
            if (fabs(diff) < NumericLimits<double>::epsilon())
                return 0;
            return diff < 0 ? -1 : 1;
        },
        [&](bool value) -> int {
            auto casted = other.to_bool();
            if (!casted.has_value())
                return 1;
            return value ^ *casted;
        },
        [&](TupleValue const& value) -> int {
            if (other.is_null() || (other.type() != SQLType::Tuple)) {
                if (value.values.size() == 1)
                    return value.values[0].compare(other);
                return 1;
            }

            auto const& other_value = other.m_value->get<TupleValue>();
            if (auto result = value.descriptor->compare_ignoring_names(*other_value.descriptor); result != 0)
                return 1;

            if (value.values.size() != other_value.values.size())
                return value.values.size() < other_value.values.size() ? -1 : 1;

            for (size_t i = 0; i < value.values.size(); ++i) {
                auto result = value.values[i].compare(other_value.values[i]);
                if (result == 0)
                    continue;

                if (value.descriptor->at(i).order == Order::Descending)
                    result = -result;
                return result;
            }

            return 0;
        });
}

bool Value::operator==(Value const& value) const
{
    return compare(value) == 0;
}

bool Value::operator==(StringView value) const
{
    return to_deprecated_string() == value;
}

bool Value::operator==(int value) const
{
    return to_int() == value;
}

bool Value::operator==(u32 value) const
{
    return to_u32() == value;
}

bool Value::operator==(double value) const
{
    return to_double() == value;
}

bool Value::operator!=(Value const& value) const
{
    return compare(value) != 0;
}

bool Value::operator<(Value const& value) const
{
    return compare(value) < 0;
}

bool Value::operator<=(Value const& value) const
{
    return compare(value) <= 0;
}

bool Value::operator>(Value const& value) const
{
    return compare(value) > 0;
}

bool Value::operator>=(Value const& value) const
{
    return compare(value) >= 0;
}

static Result invalid_type_for_numeric_operator(AST::BinaryOperator op)
{
    return { SQLCommand::Unknown, SQLErrorCode::NumericOperatorTypeMismatch, BinaryOperator_name(op) };
}

ResultOr<Value> Value::add(Value const& other) const
{
    if (auto double_maybe = to_double(); double_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value(double_maybe.value() + other_double_maybe.value());
        if (auto int_maybe = other.to_int(); int_maybe.has_value())
            return Value(double_maybe.value() + (double)int_maybe.value());
    } else if (auto int_maybe = to_int(); int_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value(other_double_maybe.value() + (double)int_maybe.value());
        if (auto other_int_maybe = other.to_int(); other_int_maybe.has_value())
            return Value(int_maybe.value() + other_int_maybe.value());
    }
    return invalid_type_for_numeric_operator(AST::BinaryOperator::Plus);
}

ResultOr<Value> Value::subtract(Value const& other) const
{
    if (auto double_maybe = to_double(); double_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value(double_maybe.value() - other_double_maybe.value());
        if (auto int_maybe = other.to_int(); int_maybe.has_value())
            return Value(double_maybe.value() - (double)int_maybe.value());
    } else if (auto int_maybe = to_int(); int_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value((double)int_maybe.value() - other_double_maybe.value());
        if (auto other_int_maybe = other.to_int(); other_int_maybe.has_value())
            return Value(int_maybe.value() - other_int_maybe.value());
    }
    return invalid_type_for_numeric_operator(AST::BinaryOperator::Minus);
}

ResultOr<Value> Value::multiply(Value const& other) const
{
    if (auto double_maybe = to_double(); double_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value(double_maybe.value() * other_double_maybe.value());
        if (auto int_maybe = other.to_int(); int_maybe.has_value())
            return Value(double_maybe.value() * (double)int_maybe.value());
    } else if (auto int_maybe = to_int(); int_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value((double)int_maybe.value() * other_double_maybe.value());
        if (auto other_int_maybe = other.to_int(); other_int_maybe.has_value())
            return Value(int_maybe.value() * other_int_maybe.value());
    }
    return invalid_type_for_numeric_operator(AST::BinaryOperator::Multiplication);
}

ResultOr<Value> Value::divide(Value const& other) const
{
    if (auto double_maybe = to_double(); double_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value(double_maybe.value() / other_double_maybe.value());
        if (auto int_maybe = other.to_int(); int_maybe.has_value())
            return Value(double_maybe.value() / (double)int_maybe.value());
    } else if (auto int_maybe = to_int(); int_maybe.has_value()) {
        if (auto other_double_maybe = other.to_double(); other_double_maybe.has_value())
            return Value((double)int_maybe.value() / other_double_maybe.value());
        if (auto other_int_maybe = other.to_int(); other_int_maybe.has_value())
            return Value(int_maybe.value() / other_int_maybe.value());
    }
    return invalid_type_for_numeric_operator(AST::BinaryOperator::Division);
}

ResultOr<Value> Value::modulo(Value const& other) const
{
    auto int_maybe_1 = to_int();
    auto int_maybe_2 = other.to_int();
    if (!int_maybe_1.has_value() || !int_maybe_2.has_value())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::Modulo);
    return Value(int_maybe_1.value() % int_maybe_2.value());
}

ResultOr<Value> Value::shift_left(Value const& other) const
{
    auto u32_maybe = to_u32();
    auto num_bytes_maybe = other.to_int();
    if (!u32_maybe.has_value() || !num_bytes_maybe.has_value())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::ShiftLeft);
    return Value(u32_maybe.value() << num_bytes_maybe.value());
}

ResultOr<Value> Value::shift_right(Value const& other) const
{
    auto u32_maybe = to_u32();
    auto num_bytes_maybe = other.to_int();
    if (!u32_maybe.has_value() || !num_bytes_maybe.has_value())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::ShiftRight);
    return Value(u32_maybe.value() >> num_bytes_maybe.value());
}

ResultOr<Value> Value::bitwise_or(Value const& other) const
{
    auto u32_maybe_1 = to_u32();
    auto u32_maybe_2 = other.to_u32();
    if (!u32_maybe_1.has_value() || !u32_maybe_2.has_value())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::BitwiseOr);
    return Value(u32_maybe_1.value() | u32_maybe_2.value());
}

ResultOr<Value> Value::bitwise_and(Value const& other) const
{
    auto u32_maybe_1 = to_u32();
    auto u32_maybe_2 = other.to_u32();
    if (!u32_maybe_1.has_value() || !u32_maybe_2.has_value())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::BitwiseAnd);
    return Value(u32_maybe_1.value() & u32_maybe_2.value());
}

static constexpr auto sql_type_null_as_flag = static_cast<u8>(SQLType::Null);

void Value::serialize(Serializer& serializer) const
{
    auto type_flags = static_cast<u8>(type());
    if (is_null())
        type_flags |= sql_type_null_as_flag;

    serializer.serialize<u8>(type_flags);

    if (is_null())
        return;

    m_value->visit(
        [&](TupleValue const& value) {
            serializer.serialize<TupleDescriptor>(*value.descriptor);
            serializer.serialize(static_cast<u32>(value.values.size()));

            for (auto const& element : value.values)
                serializer.serialize<Value>(element);
        },
        [&](auto const& value) { serializer.serialize(value); });
}

void Value::deserialize(Serializer& serializer)
{
    auto type_flags = serializer.deserialize<u8>();
    bool has_value = true;

    if ((type_flags & sql_type_null_as_flag) && (type_flags != sql_type_null_as_flag)) {
        type_flags &= ~sql_type_null_as_flag;
        has_value = false;
    }

    m_type = static_cast<SQLType>(type_flags);

    if (!has_value)
        return;

    switch (m_type) {
    case SQLType::Null:
        VERIFY_NOT_REACHED();
        break;
    case SQLType::Text:
        m_value = serializer.deserialize<DeprecatedString>();
        break;
    case SQLType::Integer:
        m_value = serializer.deserialize<int>(0);
        break;
    case SQLType::Float:
        m_value = serializer.deserialize<double>(0.0);
        break;
    case SQLType::Boolean:
        m_value = serializer.deserialize<bool>(false);
        break;
    case SQLType::Tuple: {
        auto descriptor = serializer.adopt_and_deserialize<TupleDescriptor>();
        auto size = serializer.deserialize<u32>();

        Vector<Value> values;
        values.ensure_capacity(size);

        for (size_t i = 0; i < size; ++i)
            values.unchecked_append(serializer.deserialize<Value>());

        m_value = TupleValue { move(descriptor), move(values) };
        break;
    }
    }
}

TupleElementDescriptor Value::descriptor() const
{
    return { "", "", "", type(), Order::Ascending };
}

ResultOr<NonnullRefPtr<TupleDescriptor>> Value::infer_tuple_descriptor(Vector<Value> const& values)
{
    auto descriptor = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) SQL::TupleDescriptor));
    TRY(descriptor->try_ensure_capacity(values.size()));

    for (auto const& element : values)
        descriptor->unchecked_append({ ""sv, ""sv, ""sv, element.type(), Order::Ascending });

    return descriptor;
}

}

template<>
bool IPC::encode(Encoder& encoder, SQL::Value const& value)
{
    auto type_flags = to_underlying(value.type());
    if (value.is_null())
        type_flags |= SQL::sql_type_null_as_flag;

    encoder << type_flags;
    if (value.is_null())
        return true;

    switch (value.type()) {
    case SQL::SQLType::Null:
        break;
    case SQL::SQLType::Text:
        encoder << value.to_deprecated_string();
        break;
    case SQL::SQLType::Integer:
        encoder << value.to_int().value();
        break;
    case SQL::SQLType::Float:
        encoder << value.to_double().value();
        break;
    case SQL::SQLType::Boolean:
        encoder << value.to_bool().value();
        break;
    case SQL::SQLType::Tuple:
        encoder << value.to_vector().value();
        break;
    }

    return true;
}

template<>
ErrorOr<void> IPC::decode(Decoder& decoder, SQL::Value& value)
{
    UnderlyingType<SQL::SQLType> type_flags;
    TRY(decoder.decode(type_flags));

    if ((type_flags & SQL::sql_type_null_as_flag) && (type_flags != SQL::sql_type_null_as_flag)) {
        type_flags &= ~SQL::sql_type_null_as_flag;

        value = SQL::Value(static_cast<SQL::SQLType>(type_flags));
        return {};
    }

    switch (static_cast<SQL::SQLType>(type_flags)) {
    case SQL::SQLType::Null:
        break;
    case SQL::SQLType::Text: {
        DeprecatedString text;
        TRY(decoder.decode(text));
        value = move(text);
        break;
    }
    case SQL::SQLType::Integer: {
        int number { 0 };
        TRY(decoder.decode(number));
        value = number;
        break;
    }
    case SQL::SQLType::Float: {
        double number { 0.0 };
        TRY(decoder.decode(number));
        value = number;
        break;
    }
    case SQL::SQLType::Boolean: {
        bool boolean { false };
        TRY(decoder.decode(boolean));
        value = boolean;
        break;
    }
    case SQL::SQLType::Tuple: {
        Vector<SQL::Value> tuple;
        TRY(decoder.decode(tuple));

        if (auto result = value.assign_tuple(move(tuple)); result.is_error())
            return Error::from_errno(to_underlying(result.error().error()));

        break;
    }
    }

    return {};
}
