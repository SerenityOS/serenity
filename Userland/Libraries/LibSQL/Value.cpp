/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

//#include <cmath>
#include <cstring>
#include <LibSQL/Value.h>

namespace SQL {

Value::Value(SQLType sql_type)
{
    setup(sql_type);
}

Value::Value(SQLType sql_type, ByteBuffer &buffer, size_t &offset)
{
    setup(sql_type);
    m_deserialize(buffer, offset);
    m_is_null = false;
}

Value::Value(Value const &other)
{
    setup(other.type());
    m_is_null = other.is_null();
    if (!m_is_null) {
        m_assign_value(other);
    }
}

Value::~Value()
{
}

Value& Value::operator=(Value&& other) noexcept
{
    (*this) = other;
    return (*this);
}

Value& Value::operator=(Value const& other)
{
    if (this != &other) {
        m_is_null = other.is_null();
        if (!m_is_null) {
            VERIFY(can_cast(other));
            m_assign_value(other);
        }
    }
    return (*this);
}

Value& Value::operator=(String value)
{
    m_assign_string(move(value));
    m_is_null = false;
    return (*this);
}

Value& Value::operator=(int value)
{
    m_assign_int(value);
    m_is_null = false;
    return (*this);
}

Value& Value::operator=(double value)
{
    m_assign_double(value);
    m_is_null = false;
    return (*this);
}

Value& Value::set_null()
{
    m_is_null = true;
    return (*this);
}

Optional<String> Value::to_string() const
{
    if (!m_is_null) {
        return m_to_string();
    } else {
        return {};
    }
}

Value::operator String() const
{
    auto str = to_string();
    VERIFY(str.has_value());
    return str.value();
}

Optional<int> Value::to_int() const
{
    if (!m_is_null) {
        return m_to_int();
    } else {
        return {};
    }
}

Value::operator int() const
{
    auto i = to_int();
    VERIFY(i.has_value());
    return i.value();
}

Optional<double> Value::to_double() const
{
    if (!m_is_null) {
        return m_to_double();
    } else {
        return {};
    }
}

Value::operator double() const
{
    auto dbl = to_double();
    VERIFY(dbl.has_value());
    return dbl.value();
}

SQLType Value::type() const
{
    return m_type;
}

const char* Value::type_name() const
{
    return m_type_name();
}

size_t Value::size() const
{
    return m_size();
}

bool Value::is_null() const
{
    return m_is_null;
}

bool Value::can_cast(Value const& other) const
{
    if (other.is_null()) {
        return true;
    }
    if (type() == other.type()) {
        return true;
    }
    return m_can_cast(other);
}

void Value::serialize(ByteBuffer &buffer) const
{
    VERIFY(!is_null());
    m_serialize(buffer);
}

int Value::compare(Value const& other) const
{
    return m_compare(other);
}

bool Value::operator == (Value const& other) const
{
    return m_compare(other) == 0;
}

bool Value::operator == (String const &other) const
{
    return operator String() == other;
}

bool Value::operator == (int other) const
{
    return operator int() == other;
}

bool Value::operator == (double other) const
{
    return operator double() == other;
}

bool Value::operator != (Value const& other) const
{
    return m_compare(other) != 0;
}

bool Value::operator < (Value const& other) const {
    return m_compare(other) < 0;
}

bool Value::operator <= (Value const& other) const
{
    return m_compare(other) <= 0;
}

bool Value::operator > (Value const& other) const
{
    return m_compare(other) > 0;
}

bool Value::operator >= (Value const& other) const
{
    return m_compare(other) >= 0;
}

void Value::setup(SQLType sql_type) {
    m_type = sql_type;
    switch (sql_type) {
    case Text:
        setup_text();
        break;
    case Integer:
        setup_int();
        break;
    case Float:
        setup_float();
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void Value::setup_text()
{
    m_string = "";
    m_type_name = []() { return "Text"; };
    m_size = []() { return 64 + sizeof(int); };

    m_deserialize = [&](ByteBuffer& buffer, size_t &at_offset) {
        int len;
        memcpy(&len, buffer.offset_pointer((int) at_offset), sizeof(int));
        at_offset += sizeof(int);
        m_string = String((const char *) buffer.offset_pointer((int) at_offset));
        at_offset += 64;
    };

    m_serialize = [&](ByteBuffer &buffer) {
        char zeroes[64];

        int len = min((int) m_string.length(), 63);
        buffer.append(&len, sizeof(int));
        buffer.append(m_string.characters(), len);
        memset(zeroes, 0, 64);
        buffer.append(zeroes, 64 - len);
    };

    m_assign_value = [&](Value const& other) {
        auto str = other.to_string();
        VERIFY(str.has_value());
        m_string = str.value();
    };

    m_assign_string = [&](String const& string) {
        m_string = string;
    };

    m_assign_int = [&](int i) {
        m_string = String::number(i);
    };

    m_assign_double = [&](double d) {
        m_string = String::number(d);
    };

    m_to_string = [&]() -> Optional<String> {
        return m_string;
    };

    m_to_int = [&]() -> Optional<int> {
        return m_string.to_int();
    };

    m_to_double = [&]() -> Optional<double> {
        char *end_ptr;
        double ret = strtod(m_string.characters(), &end_ptr);
        if (end_ptr == m_string.characters()) {
            return {};
        }
        return ret;
    };

    m_compare = [&](Value const& other) -> int {
        auto s1 = to_string();
        auto s2 = other.to_string();
        VERIFY(s1.has_value());
        if (!s2.has_value()) {
            return 1;
        }
        return (s1.value() == s2.value()) ? 0 : (s1.value() < s2.value()) ? -1 : 1;
    };

    m_can_cast = [](Value const&) -> bool {
        return true;
    };

}

void Value::setup_int()
{
    m_int = 0;
    m_type_name = []() { return "Integer"; };
    m_size = []() { return sizeof(int); };

    m_deserialize = [&](ByteBuffer& buffer, size_t& at_offset) {
        memcpy(&m_int, buffer.offset_pointer((int)at_offset), sizeof(int));
        at_offset += sizeof(int);
    };

    m_serialize = [&](ByteBuffer & buffer) {
        buffer.append(&m_int, sizeof(int));
    };

    m_assign_value = [&](Value const& other) {
        auto i = other.to_int();
        VERIFY(i.has_value());
        m_int = i.value();
    };

    m_assign_string = [&](String const &string) {
        auto i = string.to_int();
        VERIFY(i.has_value());
        m_double = i.value();
    };

    m_assign_int = [&](int i) {
        m_int = i;
    };

    m_assign_double = [&](double d) {
        m_double = (int) d;
    };

    m_to_string = [&]() -> Optional<String> {
        StringBuilder builder;
        builder.appendff("{}", m_int);
        return builder.build();
    };

    m_to_int = [&]() -> Optional<int> {
        return m_int;
    };

    m_to_double = [&]() -> Optional<double> {
        return (double) m_double;
    };

    m_compare = [&](Value const& other) -> int {
        auto casted = other.to_int();
        if (!casted.has_value()) {
            return 1;
        }
        return m_int - casted.value();
    };

    m_can_cast = [](Value const& other) -> bool {
        auto i = other.to_int();
        return i.has_value();
    };

}

void Value::setup_float()
{
    m_double = 0.0;
    m_type_name = []() { return "Float"; };
    m_size = []() { return sizeof(double); };

    m_deserialize = [&](ByteBuffer& buffer, size_t& at_offset) {
        memcpy(&m_double, buffer.offset_pointer((int)at_offset), sizeof(double));
        at_offset += sizeof(double);
    };

    m_serialize = [&](ByteBuffer& buffer) {
        buffer.append(&m_double, sizeof(double));
    };

    m_to_string = [&]() -> Optional<String> {
        StringBuilder builder;
        builder.appendff("{}", m_double);
        return builder.build();
    };

    m_to_int = [&]() -> Optional<int> {
        return (int) m_double;
    };

    m_to_double = [&]() -> Optional<double> {
        return m_double;
    };

    m_assign_value = [&](Value const& other) {
        auto dbl = other.to_double();
        VERIFY(dbl.has_value());
        m_double = dbl.value();
    };

    m_assign_string = [&](String const& string) {
        char* end_ptr;
        auto dbl = strtod(string.characters(), &end_ptr);
        VERIFY(end_ptr != string.characters());
        m_double = dbl;
    };

    m_assign_int = [&](int i) {
        m_double = i;
    };

    m_assign_double = [&](double d) {
        m_double = d;
    };

    m_compare = [&](Value const& other) -> int {
        auto casted = other.to_double();
        if (!casted.has_value()) {
            return 1;
        }
        // FIXME probably fails if difference is -0.5<diff<0 because
        // that will round to 0?
        return ((m_double - casted.value()) < 1e-6) ? 0 : (int) (m_double - casted.value());
    };

    m_can_cast = [](Value const& other) -> bool {
        auto dbl = other.to_double();
        return dbl.has_value();
    };

}

}
