#pragma once

#include <AK/AKString.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <LibHTML/CSS/Length.h>

class StyleValue : public RefCounted<StyleValue> {
public:
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
    static NonnullRefPtr<StringStyleValue> create(const String& string)
    {
        return adopt(*new StringStyleValue(string));
    }
    virtual ~StringStyleValue() override {}

    String to_string() const override { return m_string; }

private:
    explicit StringStyleValue(const String& string)
        : StyleValue(Type::String)
        , m_string(string)
    {
    }

    String m_string;
};

class LengthStyleValue : public StyleValue {
public:
    static NonnullRefPtr<LengthStyleValue> create(const Length& length)
    {
        return adopt(*new LengthStyleValue(length));
    }
    virtual ~LengthStyleValue() override {}

    String to_string() const override { return m_length.to_string(); }

private:
    explicit LengthStyleValue(const Length& length)
        : StyleValue(Type::Length)
        , m_length(length)
    {
    }

    Length m_length;
};
