#pragma once

#include <AK/AKString.h>
#include <AK/IPv4Address.h>
#include <AK/Optional.h>

namespace AK {

class JsonArray;
class JsonObject;
class StringBuilder;

class JsonValue {
public:
    enum class Type {
        Undefined,
        Null,
        Int,
        UnsignedInt,
#ifndef KERNEL
        Double,
#endif
        Bool,
        String,
        Array,
        Object,
    };

    static JsonValue from_string(const StringView&);

    explicit JsonValue(Type = Type::Null);
    ~JsonValue() { clear(); }

    JsonValue(const JsonValue&);
    JsonValue(JsonValue&&);

    JsonValue& operator=(const JsonValue&);
    JsonValue& operator=(JsonValue&&);

    JsonValue(int);
    JsonValue(unsigned);
    JsonValue(long unsigned);
#ifndef KERNEL
    JsonValue(double);
#endif
    JsonValue(bool);
    JsonValue(const char*);
    JsonValue(const String&);
    JsonValue(const IPv4Address&);
    JsonValue(const JsonArray&);
    JsonValue(const JsonObject&);

    String serialized() const;
    void serialize(StringBuilder&) const;

    String to_string(const String& default_value = {}) const
    {
        if (is_string())
            return as_string();
        return default_value;
    }

    Optional<IPv4Address> to_ipv4_address() const
    {
        if (!is_string())
            return {};
        return IPv4Address::from_string(as_string());
    }

    int to_int(int default_value = 0) const
    {
        if (!is_number())
            return default_value;
#ifndef KERNEL
        if (is_double())
            return (int)as_double();
#endif
        if (is_uint())
            return (int)as_uint();
        return as_int();
    }

    unsigned to_uint(unsigned default_value = 0) const
    {
        if (!is_number())
            return default_value;
#ifndef KERNEL
        if (is_double())
            return (unsigned)as_double();
#endif
        if (is_int())
            return (unsigned)as_int();
        return as_uint();
    }

    bool to_bool(bool default_value = false) const
    {
        if (!is_bool())
            return default_value;
        return as_bool();
    }

    int as_int() const
    {
        ASSERT(is_int());
        return m_value.as_int;
    }

    int as_uint() const
    {
        ASSERT(is_uint());
        return m_value.as_uint;
    }

    int as_bool() const
    {
        ASSERT(is_bool());
        return m_value.as_bool;
    }

    String as_string() const
    {
        ASSERT(is_string());
        return *m_value.as_string;
    }

    const JsonObject& as_object() const
    {
        ASSERT(is_object());
        return *m_value.as_object;
    }

    const JsonArray& as_array() const
    {
        ASSERT(is_array());
        return *m_value.as_array;
    }

#ifndef KERNEL
    double as_double() const
    {
        ASSERT(is_double());
        return m_value.as_double;
    }
#endif

    Type type() const { return m_type; }

    bool is_null() const { return m_type == Type::Null; }
    bool is_undefined() const { return m_type == Type::Undefined; }
    bool is_bool() const { return m_type == Type::Bool; }
    bool is_string() const { return m_type == Type::String; }
    bool is_int() const { return m_type == Type::Int; }
    bool is_uint() const { return m_type == Type::UnsignedInt; }
#ifndef KERNEL
    bool is_double() const { return m_type == Type::Double; }
#endif
    bool is_array() const { return m_type == Type::Array; }
    bool is_object() const { return m_type == Type::Object; }
    bool is_number() const
    {
        if (m_type == Type::Int || m_type == Type::UnsignedInt)
            return true;
#ifdef KERNEL
        return false;
#else
        return m_type == Type::Double;
#endif
    }

    u32 to_u32(u32 default_value = 0) const
    {
        if (!is_number())
            return default_value;
#ifdef KERNEL
        return (u32)m_value.as_int;
#else
        if (type() == Type::Int)
            return (u32)m_value.as_int;
        if (type() == Type::UnsignedInt)
            return m_value.as_uint;
        return (u32)m_value.as_double;
#endif
    }

private:
    void clear();
    void copy_from(const JsonValue&);

    Type m_type { Type::Undefined };

    union {
        StringImpl* as_string { nullptr };
        JsonArray* as_array;
        JsonObject* as_object;
#ifndef KERNEL
        double as_double;
#endif
        int as_int;
        unsigned int as_uint;
        bool as_bool;
    } m_value;
};

}

using AK::JsonValue;
