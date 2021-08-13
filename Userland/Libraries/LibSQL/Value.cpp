/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Value.h>
#include <cstring>

namespace SQL {

Value::Value(SQLType sql_type)
    : m_impl(0)
{
    setup(sql_type);
}

Value::Value(SQLType sql_type, ByteBuffer& buffer, size_t& offset)
    : m_impl(0)
{
    setup(sql_type);
    m_deserialize(buffer, offset);
    m_is_null = false;
}

Value::Value(Value const& other)
    : m_impl(0)
{
    setup(other.type());
    m_is_null = other.is_null();
    if (!m_is_null)
        m_assign_value(other);
}

Value::~Value()
{
}

Value const& Value::null()
{
    static Value s_null;
    return s_null;
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

Value& Value::operator=(String const& value)
{
    m_assign_string(value);
    m_is_null = false;
    return (*this);
}

Value& Value::operator=(int value)
{
    m_assign_int(value);
    m_is_null = false;
    return (*this);
}

Value& Value::operator=(u32 value)
{
    m_assign_int(static_cast<int>(value));
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
    if (!m_is_null)
        return m_to_string();
    else
        return {};
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

Optional<u32> Value::to_u32() const
{
    if (!m_is_null) {
        auto ret = m_to_int();
        if (ret.has_value())
            return static_cast<u32>(ret.value());
        else
            return {};
    } else {
        return {};
    }
}

Value::operator u32() const
{
    auto i = to_u32();
    VERIFY(i.has_value());
    return i.value();
}

Optional<double> Value::to_double() const
{
    if (!m_is_null)
        return m_to_double();
    else
        return {};
}

Value::operator double() const
{
    auto dbl = to_double();
    VERIFY(dbl.has_value());
    return dbl.value();
}

bool Value::can_cast(Value const& other) const
{
    if (other.is_null())
        return true;
    if (type() == other.type())
        return true;
    return m_can_cast(other);
}

bool Value::operator==(String const& other) const
{
    return operator String() == other;
}

bool Value::operator==(int other) const
{
    return operator int() == other;
}

bool Value::operator==(double other) const
{
    return operator double() == other;
}

void Value::setup(SQLType sql_type)
{
    m_type = sql_type;
    switch (sql_type) {
    case SQLType::Text:
        setup_text();
        break;
    case SQLType::Integer:
        setup_int();
        break;
    case SQLType::Float:
        setup_float();
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void Value::setup_text()
{
    m_impl = String("");
    m_type_name = []() { return "Text"; };
    m_size = []() { return 64 + sizeof(int); };

    m_deserialize = [&](ByteBuffer& buffer, size_t& at_offset) {
        int len;
        memcpy(&len, buffer.offset_pointer((int)at_offset), sizeof(int));
        at_offset += sizeof(int);
        m_impl = String((const char*)buffer.offset_pointer((int)at_offset));
        at_offset += 64;
    };

    m_serialize = [&](ByteBuffer& buffer) {
        char zeroes[64];

        int len = min((int)m_impl.get<String>().length(), 63);
        buffer.append(&len, sizeof(int));
        buffer.append(m_impl.get<String>().characters(), len);
        memset(zeroes, 0, 64);
        buffer.append(zeroes, 64 - len);
    };

    m_assign_value = [&](Value const& other) {
        auto str = other.to_string();
        VERIFY(str.has_value());
        m_impl = str.value();
    };

    m_assign_string = [&](String const& string) {
        m_impl = string;
    };

    m_assign_int = [&](int i) {
        m_impl = String::number(i);
    };

    m_assign_double = [&](double d) {
        m_impl = String::number(d);
    };

    m_to_string = [&]() -> Optional<String> {
        return m_impl.get<String>();
    };

    m_to_int = [&]() -> Optional<int> {
        return m_impl.get<String>().to_int();
    };

    m_to_double = [&]() -> Optional<double> {
        char* end_ptr;
        double ret = strtod(m_impl.get<String>().characters(), &end_ptr);
        if (end_ptr == m_impl.get<String>().characters()) {
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
        if (s1.value() == s2.value())
            return 0;
        return (s1.value() < s2.value()) ? -1 : 1;
    };

    m_can_cast = [](Value const&) -> bool {
        return true;
    };

    m_hash = [&]() {
        return m_impl.get<String>().hash();
    };
}

void Value::setup_int()
{
    m_impl.set<int>(0);
    m_type_name = []() { return "Integer"; };
    m_size = []() { return sizeof(int); };

    m_deserialize = [&](ByteBuffer& buffer, size_t& at_offset) {
        memcpy(m_impl.get_pointer<int>(), buffer.offset_pointer((int)at_offset), sizeof(int));
        at_offset += sizeof(int);
    };

    m_serialize = [&](ByteBuffer& buffer) {
        buffer.append(m_impl.get_pointer<int>(), sizeof(int));
    };

    m_assign_value = [&](Value const& other) {
        auto i = other.to_int();
        VERIFY(i.has_value());
        m_impl = i.value();
    };

    m_assign_string = [&](String const& string) {
        auto i = string.to_int();
        VERIFY(i.has_value());
        m_impl = i.value();
    };

    m_assign_int = [&](int i) {
        m_impl.set<int>(move(i));
    };

    m_assign_double = [&](double d) {
        m_impl.set<int>((int)d);
    };

    m_to_string = [&]() -> Optional<String> {
        StringBuilder builder;
        builder.appendff("{}", m_impl.get<int>());
        return builder.build();
    };

    m_to_int = [&]() -> Optional<int> {
        return m_impl.get<int>();
    };

    m_to_double = [&]() -> Optional<double> {
        return static_cast<double>(m_impl.get<int>());
    };

    m_compare = [&](Value const& other) -> int {
        auto casted = other.to_int();
        if (!casted.has_value()) {
            return 1;
        }
        return m_impl.get<int>() - casted.value();
    };

    m_can_cast = [](Value const& other) -> bool {
        auto i = other.to_int();
        return i.has_value();
    };

    m_hash = [&]() -> u32 {
        return int_hash(m_impl.get<int>());
    };
}

void Value::setup_float()
{
    m_impl.set<double>(0.0);
    m_type_name = []() { return "Float"; };
    m_size = []() { return sizeof(double); };

    m_deserialize = [&](ByteBuffer& buffer, size_t& at_offset) {
        memcpy(m_impl.get_pointer<double>(), buffer.offset_pointer((int)at_offset), sizeof(double));
        at_offset += sizeof(double);
    };

    m_serialize = [&](ByteBuffer& buffer) {
        buffer.append(m_impl.get_pointer<double>(), sizeof(double));
    };

    m_to_string = [&]() -> Optional<String> {
        StringBuilder builder;
        builder.appendff("{}", m_impl.get<double>());
        return builder.build();
    };

    m_to_int = [&]() -> Optional<int> {
        return (int)m_impl.get<double>();
    };

    m_to_double = [&]() -> Optional<double> {
        return m_impl.get<double>();
    };

    m_assign_value = [&](Value const& other) {
        auto dbl = other.to_double();
        VERIFY(dbl.has_value());
        m_impl.set<double>(move(dbl.value()));
    };

    m_assign_string = [&](String const& string) {
        char* end_ptr;
        auto dbl = strtod(string.characters(), &end_ptr);
        VERIFY(end_ptr != string.characters());
        m_impl.set<double>(move(dbl));
    };

    m_assign_int = [&](int i) {
        m_impl.set<double>(static_cast<double>(i));
    };

    m_assign_double = [&](double d) {
        m_impl.set<double>(move(d));
    };

    m_compare = [&](Value const& other) -> int {
        auto casted = other.to_double();
        if (!casted.has_value()) {
            return 1;
        }
        auto diff = m_impl.get<double>() - casted.value();
        return (diff < NumericLimits<double>::epsilon()) ? 0 : ((diff > 0) ? 1 : -1);
    };

    m_can_cast = [](Value const& other) -> bool {
        auto dbl = other.to_double();
        return dbl.has_value();
    };

    // Using floats in hash functions is a bad idea. Let's disable that for now.
    m_hash = []() -> u32 {
        VERIFY_NOT_REACHED();
    };
}

}
