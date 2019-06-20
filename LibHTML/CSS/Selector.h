#pragma once

#include <AK/AKString.h>
#include <AK/Vector.h>

class Selector {
public:
    Selector();
    ~Selector();

    struct Component {
        enum class Type { Invalid, TagName, Id, Class };
        Type type { Type::Invalid };
        String value;
    };

    const Vector<Component>& components() const { return m_components; }

private:
    Vector<Component> m_components;
};
