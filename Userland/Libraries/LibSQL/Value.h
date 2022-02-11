/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Result.h>
#include <LibSQL/TupleDescriptor.h>
#include <LibSQL/Type.h>
#include <LibSQL/ValueImpl.h>
#include <string.h>

namespace SQL {

/**
 * A `Value` is an atomic piece of SQL data`. A `Value` has a basic type
 * (Text/String, Integer, Float, etc). Richer types are implemented in higher
 * level layers, but the resulting data is stored in these `Value` objects.
 */
class Value {
public:
    Value(Value&) = default;
    Value(Value const&) = default;

    explicit Value(SQLType sql_type = SQLType::Null);

    template<typename... Ts>
    explicit Value(Variant<Ts...> impl)
        : m_impl(impl)
    {
    }

    enum SetImplementation {
        SetImplementationSingleton
    };

    template<typename I>
    Value(SetImplementation, I&& impl)
    {
        m_impl.set<I>(forward<I>(impl));
    }

    Value(SQLType, Value const&);
    Value(SQLType, String const&);
    Value(SQLType, char const*);
    Value(SQLType, int);
    Value(SQLType, double);
    Value(SQLType, bool);
    explicit Value(String const&);
    explicit Value(char const*);
    explicit Value(int);
    explicit Value(u32);
    explicit Value(double);
    explicit Value(bool);

    ~Value() = default;

    [[nodiscard]] bool is_null() const;
    [[nodiscard]] SQLType type() const;
    [[nodiscard]] String type_name() const;
    [[nodiscard]] BaseTypeImpl downcast_to_basetype() const;

    template<typename Impl>
    Impl const& get_impl(Badge<Impl>) const { return m_impl.get<Impl>(); }

    [[nodiscard]] String to_string() const;
    [[nodiscard]] Optional<int> to_int() const;
    [[nodiscard]] Optional<u32> to_u32() const;
    [[nodiscard]] Optional<double> to_double() const;
    [[nodiscard]] Optional<bool> to_bool() const;
    [[nodiscard]] Optional<Vector<Value>> to_vector() const;

    explicit operator String() const;
    explicit operator int() const;
    explicit operator u32() const;
    explicit operator double() const;
    explicit operator bool() const;

    void assign(Value const& other_value);
    void assign(String const& string_value);
    void assign(int int_value);
    void assign(u32 unsigned_int_value);
    void assign(double double_value);
    void assign(bool bool_value);
    void assign(Vector<Value> const& values);

    Value& operator=(Value const& other);

    Value& operator=(String const&);
    Value& operator=(char const*);
    Value& operator=(int);
    Value& operator=(u32);
    Value& operator=(double);
    Value& operator=(bool);
    Value& operator=(Vector<Value> const&);

    [[nodiscard]] size_t length() const;
    [[nodiscard]] u32 hash() const;
    [[nodiscard]] bool can_cast(Value const&) const;
    void serialize(Serializer&) const;
    void deserialize(Serializer&);

    [[nodiscard]] int compare(Value const&) const;
    bool operator==(Value const&) const;
    bool operator==(String const&) const;
    bool operator==(int) const;
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

    [[nodiscard]] TupleElementDescriptor descriptor() const
    {
        return { "", "", "", type(), Order::Ascending };
    }

    static Value const& null();
    static Value create_tuple(NonnullRefPtr<TupleDescriptor> const&);
    static Value create_array(SQLType element_type, Optional<size_t> const& max_size = {});

private:
    void setup(SQLType type);

    ValueTypeImpl m_impl { NullImpl() };
    friend Serializer;
};

}
