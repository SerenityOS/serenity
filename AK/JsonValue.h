#pragma once

#include <AK/AKString.h>

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

    Type type() const { return m_type; }

    bool is_null() const { return m_type == Type::Null; }
    bool is_undefined() const { return m_type == Type::Undefined; }
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

    dword to_dword(dword default_value = 0) const
    {
        if (!is_number())
            return default_value;
#ifdef KERNEL
        return (dword)m_value.as_int;
#else
        if (type() == Type::Int)
            return (dword)m_value.as_int;
        if (type() == Type::UnsignedInt)
            return m_value.as_uint;
        return (dword)m_value.as_double;
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
