/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022-2024, Tim Flynn <trflynn89@serenityos.org>
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

namespace SQL {

// We use the upper 4 bits of the encoded type to store extra information about the type. This
// includes if the value is null, and the encoded size of any integer type. Of course, this encoding
// only works if the SQL type itself fits in the lower 4 bits.
enum class SQLTypeWithCount {
#undef __ENUMERATE_SQL_TYPE
#define __ENUMERATE_SQL_TYPE(name, type) type,
    ENUMERATE_SQL_TYPES(__ENUMERATE_SQL_TYPE)
#undef __ENUMERATE_SQL_TYPE
        Count,
};

static_assert(to_underlying(SQLTypeWithCount::Count) <= 0x0f, "Too many SQL types for current encoding");

// Adding to this list is fine, but changing the order of any value here will result in LibSQL
// becoming unable to read existing .db files. If the order must absolutely be changed, be sure
// to bump Heap::VERSION.
enum class TypeData : u8 {
    Null = 1 << 4,
    Int8 = 2 << 4,
    Int16 = 3 << 4,
    Int32 = 4 << 4,
    Int64 = 5 << 4,
    Uint8 = 6 << 4,
    Uint16 = 7 << 4,
    Uint32 = 8 << 4,
    Uint64 = 9 << 4,
};

template<typename Callback>
static decltype(auto) downsize_integer(Integer auto value, Callback&& callback)
{
    if constexpr (IsSigned<decltype(value)>) {
        if (AK::is_within_range<i8>(value))
            return callback(static_cast<i8>(value), TypeData::Int8);
        if (AK::is_within_range<i16>(value))
            return callback(static_cast<i16>(value), TypeData::Int16);
        if (AK::is_within_range<i32>(value))
            return callback(static_cast<i32>(value), TypeData::Int32);
        return callback(value, TypeData::Int64);
    } else {
        if (AK::is_within_range<u8>(value))
            return callback(static_cast<i8>(value), TypeData::Uint8);
        if (AK::is_within_range<u16>(value))
            return callback(static_cast<i16>(value), TypeData::Uint16);
        if (AK::is_within_range<u32>(value))
            return callback(static_cast<i32>(value), TypeData::Uint32);
        return callback(value, TypeData::Uint64);
    }
}

template<typename Callback>
static decltype(auto) downsize_integer(Value const& value, Callback&& callback)
{
    VERIFY(value.is_int());

    if (value.value().has<i64>())
        return downsize_integer(value.value().get<i64>(), forward<Callback>(callback));
    return downsize_integer(value.value().get<u64>(), forward<Callback>(callback));
}

template<typename Callback>
static ResultOr<Value> perform_integer_operation(Value const& lhs, Value const& rhs, Callback&& callback)
{
    VERIFY(lhs.is_int());
    VERIFY(rhs.is_int());

    if (lhs.value().has<i64>()) {
        if (auto rhs_value = rhs.to_int<i64>(); rhs_value.has_value())
            return callback(lhs.to_int<i64>().value(), rhs_value.value());
    } else {
        if (auto rhs_value = rhs.to_int<u64>(); rhs_value.has_value())
            return callback(lhs.to_int<u64>().value(), rhs_value.value());
    }

    return Result { SQLCommand::Unknown, SQLErrorCode::IntegerOverflow };
}

Value::Value(SQLType type)
    : m_type(type)
{
}

Value::Value(String value)
    : Value(value.to_byte_string())
{
}

Value::Value(ByteString value)
    : m_type(SQLType::Text)
    , m_value(move(value))
{
}

Value::Value(double value)
{
    if (trunc(value) == value) {
        if (AK::is_within_range<i64>(value)) {
            m_type = SQLType::Integer;
            m_value = static_cast<i64>(value);
            return;
        }
        if (AK::is_within_range<u64>(value)) {
            m_type = SQLType::Integer;
            m_value = static_cast<u64>(value);
            return;
        }
    }

    m_type = SQLType::Float;
    m_value = value;
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

Value::Value(Duration duration)
    : m_type(SQLType::Integer)
    , m_value(duration.to_milliseconds())
{
}

Value::Value(UnixDateTime time)
    : Value(time.offset_to_epoch())
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
#define __ENUMERATE_SQL_TYPE(name, type) \
    case SQLType::type:                  \
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

bool Value::is_int() const
{
    return m_value.has_value() && (m_value->has<i64>() || m_value->has<u64>());
}

ErrorOr<String> Value::to_string() const
{
    if (is_null())
        return String::from_utf8("(null)"sv);

    return m_value->visit(
        [](ByteString const& value) { return String::from_byte_string(value); },
        [](Integer auto value) -> ErrorOr<String> { return String::number(value); },
        [](double value) -> ErrorOr<String> { return String::number(value); },
        [](bool value) { return String::from_utf8(value ? "true"sv : "false"sv); },
        [](TupleValue const& value) {
            StringBuilder builder;

            builder.append('(');
            builder.join(',', value.values);
            builder.append(')');

            return builder.to_string();
        });
}

ByteString Value::to_byte_string() const
{
    if (is_null())
        return "(null)"sv;

    return m_value->visit(
        [](ByteString const& value) -> ByteString { return value; },
        [](Integer auto value) -> ByteString { return ByteString::number(value); },
        [](double value) -> ByteString { return ByteString::number(value); },
        [](bool value) -> ByteString { return value ? "true"sv : "false"sv; },
        [](TupleValue const& value) -> ByteString {
            StringBuilder builder;

            builder.append('(');
            builder.join(',', value.values);
            builder.append(')');

            return builder.to_byte_string();
        });
}

Optional<double> Value::to_double() const
{
    if (is_null())
        return {};

    return m_value->visit(
        [](ByteString const& value) -> Optional<double> { return value.to_number<double>(); },
        [](Integer auto value) -> Optional<double> { return static_cast<double>(value); },
        [](double value) -> Optional<double> { return value; },
        [](bool value) -> Optional<double> { return static_cast<double>(value); },
        [](TupleValue const&) -> Optional<double> { return {}; });
}

Optional<bool> Value::to_bool() const
{
    if (is_null())
        return {};

    return m_value->visit(
        [](ByteString const& value) -> Optional<bool> {
            if (value.equals_ignoring_ascii_case("true"sv) || value.equals_ignoring_ascii_case("t"sv))
                return true;
            if (value.equals_ignoring_ascii_case("false"sv) || value.equals_ignoring_ascii_case("f"sv))
                return false;
            return {};
        },
        [](Integer auto value) -> Optional<bool> { return static_cast<bool>(value); },
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

Optional<UnixDateTime> Value::to_unix_date_time() const
{
    auto time = to_int<i64>();
    if (!time.has_value())
        return {};

    return UnixDateTime::from_milliseconds_since_epoch(*time);
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

Value& Value::operator=(ByteString value)
{
    m_type = SQLType::Text;
    m_value = move(value);
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
        [](ByteString const& value) -> size_t { return sizeof(u32) + value.length(); },
        [](Integer auto value) -> size_t {
            return downsize_integer(value, [](auto integer, auto) {
                return sizeof(integer);
            });
        },
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
        [](ByteString const& value) -> u32 { return value.hash(); },
        [](Integer auto value) -> u32 {
            return downsize_integer(value, [](auto integer, auto) {
                if constexpr (sizeof(decltype(integer)) == 8)
                    return u64_hash(integer);
                else
                    return int_hash(integer);
            });
        },
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
        [&](ByteString const& value) -> int { return value.view().compare(other.to_byte_string()); },
        [&](Integer auto value) -> int {
            auto casted = other.to_int<IntegerType<decltype(value)>>();
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
    return to_byte_string() == value;
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

template<typename Operator>
static Result invalid_type_for_numeric_operator(Operator op)
{
    if constexpr (IsSame<Operator, AST::BinaryOperator>)
        return { SQLCommand::Unknown, SQLErrorCode::NumericOperatorTypeMismatch, BinaryOperator_name(op) };
    else if constexpr (IsSame<Operator, AST::UnaryOperator>)
        return { SQLCommand::Unknown, SQLErrorCode::NumericOperatorTypeMismatch, UnaryOperator_name(op) };
    else
        static_assert(DependentFalse<Operator>);
}

ResultOr<Value> Value::add(Value const& other) const
{
    if (is_int() && other.is_int()) {
        return perform_integer_operation(*this, other, [](auto lhs, auto rhs) -> ResultOr<Value> {
            Checked result { lhs };
            result.add(rhs);

            if (result.has_overflow())
                return Result { SQLCommand::Unknown, SQLErrorCode::IntegerOverflow };
            return Value { result.value_unchecked() };
        });
    }

    auto lhs = to_double();
    auto rhs = other.to_double();

    if (!lhs.has_value() || !rhs.has_value())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::Plus);
    return Value { lhs.value() + rhs.value() };
}

ResultOr<Value> Value::subtract(Value const& other) const
{
    if (is_int() && other.is_int()) {
        return perform_integer_operation(*this, other, [](auto lhs, auto rhs) -> ResultOr<Value> {
            Checked result { lhs };
            result.sub(rhs);

            if (result.has_overflow())
                return Result { SQLCommand::Unknown, SQLErrorCode::IntegerOverflow };
            return Value { result.value_unchecked() };
        });
    }

    auto lhs = to_double();
    auto rhs = other.to_double();

    if (!lhs.has_value() || !rhs.has_value())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::Minus);
    return Value { lhs.value() - rhs.value() };
}

ResultOr<Value> Value::multiply(Value const& other) const
{
    if (is_int() && other.is_int()) {
        return perform_integer_operation(*this, other, [](auto lhs, auto rhs) -> ResultOr<Value> {
            Checked result { lhs };
            result.mul(rhs);

            if (result.has_overflow())
                return Result { SQLCommand::Unknown, SQLErrorCode::IntegerOverflow };
            return Value { result.value_unchecked() };
        });
    }

    auto lhs = to_double();
    auto rhs = other.to_double();

    if (!lhs.has_value() || !rhs.has_value())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::Multiplication);
    return Value { lhs.value() * rhs.value() };
}

ResultOr<Value> Value::divide(Value const& other) const
{
    auto lhs = to_double();
    auto rhs = other.to_double();

    if (!lhs.has_value() || !rhs.has_value())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::Division);
    if (rhs == 0.0)
        return Result { SQLCommand::Unknown, SQLErrorCode::IntegerOverflow };

    return Value { lhs.value() / rhs.value() };
}

ResultOr<Value> Value::modulo(Value const& other) const
{
    if (!is_int() || !other.is_int())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::Modulo);

    return perform_integer_operation(*this, other, [](auto lhs, auto rhs) -> ResultOr<Value> {
        Checked result { lhs };
        result.mod(rhs);

        if (result.has_overflow())
            return Result { SQLCommand::Unknown, SQLErrorCode::IntegerOverflow };
        return Value { result.value_unchecked() };
    });
}

ResultOr<Value> Value::negate() const
{
    if (type() == SQLType::Integer) {
        auto value = to_int<i64>();
        if (!value.has_value())
            return invalid_type_for_numeric_operator(AST::UnaryOperator::Minus);

        return Value { value.value() * -1 };
    }

    if (type() == SQLType::Float)
        return Value { -to_double().value() };

    return invalid_type_for_numeric_operator(AST::UnaryOperator::Minus);
}

ResultOr<Value> Value::shift_left(Value const& other) const
{
    if (!is_int() || !other.is_int())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::ShiftLeft);

    return perform_integer_operation(*this, other, [](auto lhs, auto rhs) -> ResultOr<Value> {
        using LHS = decltype(lhs);
        using RHS = decltype(rhs);

        static constexpr auto max_shift = static_cast<RHS>(sizeof(LHS) * 8);
        if (rhs < 0 || rhs >= max_shift)
            return Result { SQLCommand::Unknown, SQLErrorCode::IntegerOverflow };

        return Value { lhs << rhs };
    });
}

ResultOr<Value> Value::shift_right(Value const& other) const
{
    if (!is_int() || !other.is_int())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::ShiftRight);

    return perform_integer_operation(*this, other, [](auto lhs, auto rhs) -> ResultOr<Value> {
        using LHS = decltype(lhs);
        using RHS = decltype(rhs);

        static constexpr auto max_shift = static_cast<RHS>(sizeof(LHS) * 8);
        if (rhs < 0 || rhs >= max_shift)
            return Result { SQLCommand::Unknown, SQLErrorCode::IntegerOverflow };

        return Value { lhs >> rhs };
    });
}

ResultOr<Value> Value::bitwise_or(Value const& other) const
{
    if (!is_int() || !other.is_int())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::BitwiseOr);

    return perform_integer_operation(*this, other, [](auto lhs, auto rhs) {
        return Value { lhs | rhs };
    });
}

ResultOr<Value> Value::bitwise_and(Value const& other) const
{
    if (!is_int() || !other.is_int())
        return invalid_type_for_numeric_operator(AST::BinaryOperator::BitwiseAnd);

    return perform_integer_operation(*this, other, [](auto lhs, auto rhs) {
        return Value { lhs & rhs };
    });
}

ResultOr<Value> Value::bitwise_not() const
{
    if (!is_int())
        return invalid_type_for_numeric_operator(AST::UnaryOperator::BitwiseNot);

    return downsize_integer(*this, [](auto value, auto) {
        return Value { ~value };
    });
}

static u8 encode_type_flags(Value const& value)
{
    auto type_flags = to_underlying(value.type());

    if (value.is_null()) {
        type_flags |= to_underlying(TypeData::Null);
    } else if (value.is_int()) {
        downsize_integer(value, [&](auto, auto type_data) {
            type_flags |= to_underlying(type_data);
        });
    }

    return type_flags;
}

void Value::serialize(Serializer& serializer) const
{
    auto type_flags = encode_type_flags(*this);
    serializer.serialize<u8>(type_flags);

    if (is_null())
        return;

    if (is_int()) {
        downsize_integer(*this, [&](auto integer, auto) {
            serializer.serialize(integer);
        });
        return;
    }

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

    auto type_data = static_cast<TypeData>(type_flags & 0xf0);
    m_type = static_cast<SQLType>(type_flags & 0x0f);

    if (type_data == TypeData::Null)
        return;

    switch (m_type) {
    case SQLType::Null:
        VERIFY_NOT_REACHED();
    case SQLType::Text:
        m_value = serializer.deserialize<ByteString>();
        break;
    case SQLType::Integer:
        switch (type_data) {
        case TypeData::Int8:
            m_value = static_cast<i64>(serializer.deserialize<i8>(0));
            break;
        case TypeData::Int16:
            m_value = static_cast<i64>(serializer.deserialize<i16>(0));
            break;
        case TypeData::Int32:
            m_value = static_cast<i64>(serializer.deserialize<i32>(0));
            break;
        case TypeData::Int64:
            m_value = static_cast<i64>(serializer.deserialize<i64>(0));
            break;
        case TypeData::Uint8:
            m_value = static_cast<u64>(serializer.deserialize<u8>(0));
            break;
        case TypeData::Uint16:
            m_value = static_cast<u64>(serializer.deserialize<u16>(0));
            break;
        case TypeData::Uint32:
            m_value = static_cast<u64>(serializer.deserialize<u32>(0));
            break;
        case TypeData::Uint64:
            m_value = static_cast<u64>(serializer.deserialize<u64>(0));
            break;
        default:
            VERIFY_NOT_REACHED();
        }
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
ErrorOr<void> IPC::encode(Encoder& encoder, SQL::Value const& value)
{
    auto type_flags = encode_type_flags(value);
    TRY(encoder.encode(type_flags));

    if (value.is_null())
        return {};

    switch (value.type()) {
    case SQL::SQLType::Null:
        return {};
    case SQL::SQLType::Text:
        return encoder.encode(value.to_byte_string());
    case SQL::SQLType::Integer:
        return SQL::downsize_integer(value, [&](auto integer, auto) {
            return encoder.encode(integer);
        });
    case SQL::SQLType::Float:
        return encoder.encode(value.to_double().value());
    case SQL::SQLType::Boolean:
        return encoder.encode(value.to_bool().value());
    case SQL::SQLType::Tuple:
        return encoder.encode(value.to_vector().value());
    }

    VERIFY_NOT_REACHED();
}

template<>
ErrorOr<SQL::Value> IPC::decode(Decoder& decoder)
{
    auto type_flags = TRY(decoder.decode<u8>());

    auto type_data = static_cast<SQL::TypeData>(type_flags & 0xf0);
    auto type = static_cast<SQL::SQLType>(type_flags & 0x0f);

    if (type_data == SQL::TypeData::Null)
        return SQL::Value { type };

    switch (type) {
    case SQL::SQLType::Null:
        return SQL::Value {};
    case SQL::SQLType::Text:
        return SQL::Value { TRY(decoder.decode<ByteString>()) };
    case SQL::SQLType::Integer:
        switch (type_data) {
        case SQL::TypeData::Int8:
            return SQL::Value { TRY(decoder.decode<i8>()) };
        case SQL::TypeData::Int16:
            return SQL::Value { TRY(decoder.decode<i16>()) };
        case SQL::TypeData::Int32:
            return SQL::Value { TRY(decoder.decode<i32>()) };
        case SQL::TypeData::Int64:
            return SQL::Value { TRY(decoder.decode<i64>()) };
        case SQL::TypeData::Uint8:
            return SQL::Value { TRY(decoder.decode<u8>()) };
        case SQL::TypeData::Uint16:
            return SQL::Value { TRY(decoder.decode<u16>()) };
        case SQL::TypeData::Uint32:
            return SQL::Value { TRY(decoder.decode<u32>()) };
        case SQL::TypeData::Uint64:
            return SQL::Value { TRY(decoder.decode<u64>()) };
        default:
            break;
        }
        break;
    case SQL::SQLType::Float:
        return SQL::Value { TRY(decoder.decode<double>()) };
    case SQL::SQLType::Boolean:
        return SQL::Value { TRY(decoder.decode<bool>()) };
    case SQL::SQLType::Tuple: {
        auto tuple = TRY(decoder.decode<Vector<SQL::Value>>());
        auto value = SQL::Value::create_tuple(move(tuple));

        if (value.is_error())
            return Error::from_errno(to_underlying(value.error().error()));

        return value.release_value();
    }
    }

    VERIFY_NOT_REACHED();
}
