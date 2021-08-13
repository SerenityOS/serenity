/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <LibSQL/Type.h>

namespace SQL {

/**
 * A `Value` is an atomic piece of SQL data. A `Value` has a basic type
 * (Text/String, Integer, Float, etc). Richer types are implemented in higher
 * level layers, but the resulting data is stored in these `Value` objects.
 */
class Value {
public:
    explicit Value(SQLType sql_type = SQLType::Text);
    Value(SQLType sql_type, ByteBuffer& buffer, size_t& offset);
    Value(Value const& other);
    ~Value();

    static Value const& null();

    Value& operator=(Value&& other) noexcept
    {
        (*this) = other;
        return (*this);
    }
    Value& operator=(Value const& other);
    Value& operator=(String const&);
    Value& operator=(String&& string)
    {
        operator=(string);
        return *this;
    }
    Value& operator=(int);
    Value& operator=(u32);
    Value& operator=(double);
    Value& set_null();

    Optional<String> to_string() const;
    explicit operator String() const;
    Optional<int> to_int() const;
    explicit operator int() const;
    Optional<double> to_double() const;
    explicit operator double() const;
    Optional<u32> to_u32() const;
    explicit operator u32() const;

    [[nodiscard]] SQLType type() const { return m_type; }
    [[nodiscard]] const char* type_name() const { return m_type_name(); }
    [[nodiscard]] size_t size() const { return m_size(); }
    [[nodiscard]] int compare(Value const& other) const { return m_compare(other); }
    [[nodiscard]] bool is_null() const { return m_is_null; }
    [[nodiscard]] bool can_cast(Value const&) const;
    [[nodiscard]] u32 hash() const { return (is_null()) ? 0 : m_hash(); }

    bool operator==(Value const& other) const { return m_compare(other) == 0; }
    bool operator==(String const& other) const;
    bool operator==(int other) const;
    bool operator==(double other) const;
    bool operator!=(Value const& other) const { return m_compare(other) != 0; }
    bool operator<(Value const& other) const { return m_compare(other) < 0; }
    bool operator<=(Value const& other) const { return m_compare(other) <= 0; }
    bool operator>(Value const& other) const { return m_compare(other) > 0; }
    bool operator>=(Value const& other) const { return m_compare(other) >= 0; }

    void serialize(ByteBuffer& buffer) const
    {
        VERIFY(!is_null());
        m_serialize(buffer);
    }

private:
    void setup(SQLType sql_type);
    void setup_text();
    void setup_int();
    void setup_float();

    Function<Optional<String>()> m_to_string;
    Function<Optional<int>()> m_to_int;
    Function<Optional<double>()> m_to_double;
    Function<void(Value const&)> m_assign_value;
    Function<void(String const&)> m_assign_string;
    Function<void(int)> m_assign_int;
    Function<void(double)> m_assign_double;
    Function<int(Value const&)> m_compare;
    Function<void(ByteBuffer&)> m_serialize;
    Function<void(ByteBuffer&, size_t& offset)> m_deserialize;
    Function<size_t()> m_size;
    Function<const char*()> m_type_name;
    Function<bool(Value const&)> m_can_cast;
    Function<u32()> m_hash;

    SQLType m_type { SQLType::Text };
    bool m_is_null { true };

    Variant<String, int, double> m_impl;
};

}
