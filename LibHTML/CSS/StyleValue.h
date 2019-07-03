#pragma once

#include <AK/AKString.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <LibHTML/CSS/Length.h>

class StyleValue : public RefCounted<StyleValue> {
public:
    static NonnullRefPtr<StyleValue> parse(const StringView&);

    virtual ~StyleValue();

    enum class Type {
        Invalid,
        Inherit,
        Initial,
        String,
        Length,
    };

    Type type() const { return m_type; }

    virtual String to_string() const = 0;

protected:
    explicit StyleValue(Type);

private:
    Type m_type { Type::Invalid };
};

class StringStyleValue : public StyleValue {
public:
    virtual ~StringStyleValue() override {}
    StringStyleValue(const String& string)
        : StyleValue(Type::String)
        , m_string(string)
    {
    }

    String to_string() const override { return m_string; }

private:
    String m_string;
};

class LengthStyleValue : public StyleValue {
public:
    virtual ~LengthStyleValue() override {}
    LengthStyleValue(const Length& length)
        : StyleValue(Type::Length)
        , m_length(length)
    {
    }

    String to_string() const override { return m_length.to_string(); }

private:
    Length m_length;
};
