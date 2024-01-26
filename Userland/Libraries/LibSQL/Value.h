/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022-2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Checked.h>
#include <AK/Format.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibIPC/Forward.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Result.h>
#include <LibSQL/Type.h>
#include <math.h>

namespace SQL {

template<typename T>
concept Boolean = SameAs<RemoveCVReference<T>, bool>;

template<typename T>
concept Integer = (Integral<T> && !Boolean<T>);

/**
 * A `Value` is an atomic piece of SQL data`. A `Value` has a basic type
 * (Text/String, Integer, Float, etc). Richer types are implemented in higher
 * level layers, but the resulting data is stored in these `Value` objects.
 */
class Value {
    template<Integer T>
    using IntegerType = Conditional<IsSigned<T>, i64, u64>;

public:
    explicit Value(SQLType sql_type = SQLType::Null);
    explicit Value(String);
    explicit Value(ByteString);
    explicit Value(double);
    Value(Value const&);
    Value(Value&&);
    ~Value();

    explicit Value(Integer auto value)
        : m_type(SQLType::Integer)
        , m_value(static_cast<IntegerType<decltype(value)>>(value))
    {
    }

    explicit Value(Boolean auto value)
        : m_type(SQLType::Boolean)
        , m_value(value)
    {
    }

    explicit Value(UnixDateTime);
    explicit Value(Duration);

    static ResultOr<Value> create_tuple(NonnullRefPtr<TupleDescriptor>);
    static ResultOr<Value> create_tuple(Vector<Value>);

    [[nodiscard]] SQLType type() const;
    [[nodiscard]] StringView type_name() const;
    [[nodiscard]] bool is_type_compatible_with(SQLType) const;
    [[nodiscard]] bool is_null() const;
    [[nodiscard]] bool is_int() const;

    [[nodiscard]] auto const& value() const
    {
        return *m_value;
    }

    [[nodiscard]] ErrorOr<String> to_string() const;
    [[nodiscard]] ByteString to_byte_string() const;
    [[nodiscard]] Optional<double> to_double() const;
    [[nodiscard]] Optional<bool> to_bool() const;
    [[nodiscard]] Optional<UnixDateTime> to_unix_date_time() const;
    [[nodiscard]] Optional<Vector<Value>> to_vector() const;

    template<Integer T>
    [[nodiscard]] Optional<T> to_int() const
    {
        if (is_null())
            return {};

        return m_value->visit(
            [](ByteString const& value) -> Optional<T> {
                return value.to_number<T>();
            },
            [](Integer auto value) -> Optional<T> {
                if (!AK::is_within_range<T>(value))
                    return {};
                return static_cast<T>(value);
            },
            [](double value) -> Optional<T> {
                if (!AK::is_within_range<T>(value))
                    return {};
                return static_cast<T>(round(value));
            },
            [](bool value) -> Optional<T> { return static_cast<T>(value); },
            [](TupleValue const&) -> Optional<T> { return {}; });
    }

    Value& operator=(Value);
    Value& operator=(ByteString);
    Value& operator=(double);

    Value& operator=(Integer auto value)
    {
        m_type = SQLType::Integer;
        m_value = static_cast<IntegerType<decltype(value)>>(value);
        return *this;
    }

    ResultOr<void> assign_tuple(NonnullRefPtr<TupleDescriptor>);
    ResultOr<void> assign_tuple(Vector<Value>);

    Value& operator=(Boolean auto value)
    {
        m_type = SQLType::Boolean;
        m_value = value;
        return *this;
    }

    [[nodiscard]] size_t length() const;
    [[nodiscard]] u32 hash() const;
    void serialize(Serializer&) const;
    void deserialize(Serializer&);

    [[nodiscard]] int compare(Value const&) const;
    bool operator==(Value const&) const;
    bool operator==(StringView) const;
    bool operator==(double) const;

    template<Integer T>
    bool operator==(T value)
    {
        return to_int<T>() == value;
    }

    bool operator!=(Value const&) const;
    bool operator<(Value const&) const;
    bool operator<=(Value const&) const;
    bool operator>(Value const&) const;
    bool operator>=(Value const&) const;

    ResultOr<Value> add(Value const&) const;
    ResultOr<Value> subtract(Value const&) const;
    ResultOr<Value> multiply(Value const&) const;
    ResultOr<Value> divide(Value const&) const;
    ResultOr<Value> modulo(Value const&) const;
    ResultOr<Value> negate() const;
    ResultOr<Value> shift_left(Value const&) const;
    ResultOr<Value> shift_right(Value const&) const;
    ResultOr<Value> bitwise_or(Value const&) const;
    ResultOr<Value> bitwise_and(Value const&) const;
    ResultOr<Value> bitwise_not() const;

    [[nodiscard]] TupleElementDescriptor descriptor() const;

private:
    friend Serializer;

    struct TupleValue {
        NonnullRefPtr<TupleDescriptor> descriptor;
        Vector<Value> values;
    };

    using ValueType = Variant<ByteString, i64, u64, double, bool, TupleValue>;

    static ResultOr<NonnullRefPtr<TupleDescriptor>> infer_tuple_descriptor(Vector<Value> const& values);
    Value(NonnullRefPtr<TupleDescriptor> descriptor, Vector<Value> values);

    SQLType m_type { SQLType::Null };
    Optional<ValueType> m_value;
};

}

template<>
struct AK::Formatter<SQL::Value> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, SQL::Value const& value)
    {
        return Formatter<StringView>::format(builder, value.to_byte_string());
    }
};

namespace IPC {

template<>
ErrorOr<void> encode(Encoder&, SQL::Value const&);

template<>
ErrorOr<SQL::Value> decode(Decoder&);

}
