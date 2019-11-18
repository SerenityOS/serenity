#pragma once

#include <AK/String.h>

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
            return "[Length/auto]";
        return String::format("%d [Length/px]", m_value);
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

inline const LogStream& operator<<(const LogStream& stream, const Length& value)
{
    return stream << value.to_string();
}
