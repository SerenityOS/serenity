#pragma once

#include <AK/Retainable.h>

class StyleValue : public RefCounted<StyleValue> {
public:
    virtual ~StyleValue();

    enum Type {
        Invalid,
        Inherit,
        Initial,
        Primitive,
    };

    Type type() const { return m_type; }

protected:
    explicit StyleValue(Type);

private:
    Type m_type { Type::Invalid };
};
