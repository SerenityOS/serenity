/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Result.h>
#include <LibSQL/Type.h>

namespace SQL {

/**
 * A `Value` is an atomic piece of SQL data`. A `Value` has a basic type
 * (Text/String, Integer, Float, etc). Richer types are implemented in higher
 * level layers, but the resulting data is stored in these `Value` objects.
 */
class Value {
public:
    explicit Value(SQLType sql_type = SQLType::Null);
    explicit Value(String);
    explicit Value(int);
    explicit Value(u32);
    explicit Value(double);
    Value(Value const&);
    Value(Value&&);
    ~Value();

    static ResultOr<Value> create_tuple(NonnullRefPtr<TupleDescriptor>);
    static ResultOr<Value> create_tuple(Vector<Value>);

    template<typename T>
    requires(SameAs<RemoveCVReference<T>, bool>) explicit Value(T value)
        : m_type(SQLType::Boolean)
        , m_value(value)
    {
    }

    [[nodiscard]] SQLType type() const;
    [[nodiscard]] StringView type_name() const;
    [[nodiscard]] bool is_null() const;

    [[nodiscard]] String to_string() const;
    [[nodiscard]] Optional<int> to_int() const;
    [[nodiscard]] Optional<u32> to_u32() const;
    [[nodiscard]] Optional<double> to_double() const;
    [[nodiscard]] Optional<bool> to_bool() const;
    [[nodiscard]] Optional<Vector<Value>> to_vector() const;

    Value& operator=(Value);
    Value& operator=(String);
    Value& operator=(int);
    Value& operator=(u32);
    Value& operator=(double);

    ResultOr<void> assign_tuple(NonnullRefPtr<TupleDescriptor>);
    ResultOr<void> assign_tuple(Vector<Value>);

    template<typename T>
    requires(SameAs<RemoveCVReference<T>, bool>) Value& operator=(T value)
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
    bool operator==(int) const;
    bool operator==(u32) const;
    bool operator==(double) const;
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
    ResultOr<Value> shift_left(Value const&) const;
    ResultOr<Value> shift_right(Value const&) const;
    ResultOr<Value> bitwise_or(Value const&) const;
    ResultOr<Value> bitwise_and(Value const&) const;

    [[nodiscard]] TupleElementDescriptor descriptor() const;

private:
    friend Serializer;

    struct TupleValue {
        NonnullRefPtr<TupleDescriptor> descriptor;
        Vector<Value> values;
    };

    using ValueType = Variant<String, int, double, bool, TupleValue>;

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
        return Formatter<StringView>::format(builder, value.to_string());
    }
};
