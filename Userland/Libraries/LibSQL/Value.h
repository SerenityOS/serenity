/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/String.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Type.h>

namespace SQL {

class Value {
public:
    explicit Value(SQLType sql_type = Text);
    Value(SQLType sql_type, ByteBuffer &buffer, size_t &offset);
    Value(Value const &other);
    ~Value();

    Value& operator=(Value&& other) noexcept;
    Value& operator=(Value const& other);
    Value& operator=(String);
    Value& operator=(int);
    Value& operator=(double);
    Value& set_null();

    Optional<String> to_string() const;
    explicit operator String() const;
    Optional<int> to_int() const;
    explicit operator int() const;
    Optional<double> to_double() const;
    explicit operator double() const;

    [[nodiscard]] SQLType type() const;
    [[nodiscard]] const char* type_name() const;
    [[nodiscard]] size_t size() const;
    [[nodiscard]] int compare(Value const &other) const;
    [[nodiscard]] bool is_null() const;
    [[nodiscard]] bool can_cast(Value const&) const;

    bool operator == (Value const&) const;
    bool operator == (String const &) const;
    bool operator == (int) const;
    bool operator == (double) const;
    bool operator != (Value const&) const;
    bool operator < (Value const&) const;
    bool operator <= (Value const&) const;
    bool operator > (Value const&) const;
    bool operator >= (Value const&) const;

    void serialize(ByteBuffer &) const;

private:
    void setup(SQLType sql_type);
    void setup_text();
    void setup_int();
    void setup_float();

    Function<Optional<String>()> m_to_string;
    Function<Optional<int>()> m_to_int;
    Function<Optional<double>()> m_to_double;
    Function<void(Value const&)> m_assign_value;
    Function<void(String const &)> m_assign_string;
    Function<void(int)> m_assign_int;
    Function<void(double)> m_assign_double;
    Function<int(Value const&)> m_compare;
    Function<void(ByteBuffer&)> m_serialize;
    Function<void(ByteBuffer&, size_t &offset)> m_deserialize;
    Function<size_t()> m_size;
    Function<const char *()> m_type_name;
    Function<bool(Value const&)> m_can_cast;

    SQLType m_type { SQLType::Text };
    bool m_is_null { true };

    union {
        String m_string = "";
        int m_int;
        double m_double;
    };
};

}
