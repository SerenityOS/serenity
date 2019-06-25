#pragma once

#include <AK/AKString.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>

class StyleValue : public RefCounted<StyleValue> {
public:
    static NonnullRefPtr<StyleValue> parse(const StringView&);

    virtual ~StyleValue();

    enum Type {
        Invalid,
        Inherit,
        Initial,
        Primitive,
    };

    Type type() const { return m_type; }

    virtual String to_string() const = 0;

protected:
    explicit StyleValue(Type);

private:
    Type m_type { Type::Invalid };
};

class PrimitiveStyleValue : public StyleValue {
public:
    virtual ~PrimitiveStyleValue() override {}
    PrimitiveStyleValue(const String& string)
        : StyleValue(Type::Primitive)
        , m_string(string)
    {
    }

    String to_string() const override { return m_string; }

private:
    String m_string;
};
