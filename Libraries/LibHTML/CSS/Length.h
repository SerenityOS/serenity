#pragma once

#include <AK/AKString.h>

class Length {
public:
    enum class Type {
        Auto,
        Absolute,
    };

    Length() {}
    Length(int value, Type type)
        : m_type(type)
        , m_value(value)
    {
    }
    ~Length() {}

    bool is_auto() const { return m_type == Type::Auto; }
    bool is_absolute() const { return m_type == Type::Absolute; }

    int value() const { return m_value; }

    String to_string() const
    {
        if (is_auto())
            return "auto";
        return String::format("%d [Length/Absolute]", m_value);
    }

    int to_px() const
    {
        if (is_auto())
            return 0;
        return m_value;
    }

private:
    Type m_type { Type::Auto };
    int m_value { 0 };
};
