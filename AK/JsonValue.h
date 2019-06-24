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
        Double,
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
    JsonValue(double);
    JsonValue(bool);
    JsonValue(const char*);
    JsonValue(const String&);
    JsonValue(const JsonArray&);
    JsonValue(const JsonObject&);

    String serialized() const;
    void serialize(StringBuilder&) const;

    String as_string() const
    {
        if (m_type == Type::String)
           return *m_value.as_string;
        return { };
    }

    Type type() const { return m_type; }

    bool is_null() const { return m_type == Type::Null; }
    bool is_undefined() const { return m_type == Type::Undefined; }
    bool is_string() const { return m_type == Type::String; }
    bool is_int() const { return m_type == Type::Int; }
    bool is_double() const { return m_type == Type::Double; }
    bool is_array() const { return m_type == Type::Array; }
    bool is_object() const { return m_type == Type::Object; }
    bool is_number() const { return m_type == Type::Int || m_type == Type::Double; }

    dword to_dword(dword default_value = 0) const
    {
        if (!is_number())
            return default_value;
        if (type() == Type::Int)
            return (dword)m_value.as_int;
        return (dword)m_value.as_double;
    }

private:
    void clear();
    void copy_from(const JsonValue&);

    Type m_type { Type::Undefined };

    union {
        StringImpl* as_string { nullptr };
        JsonArray* as_array;
        JsonObject* as_object;
        double as_double;
        int as_int;
        bool as_bool;
    } m_value;
};

}

using AK::JsonValue;
