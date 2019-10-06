#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibHTML/CSS/Specificity.h>

class Selector {
public:
    struct Component {
        enum class Type {
            Invalid,
            TagName,
            Id,
            Class,
        };
        Type type { Type::Invalid };

        enum class Relation {
            None,
            ImmediateChild,
        };
        Relation relation { Relation::None };

        String value;
    };

    explicit Selector(Vector<Component>&&);
    ~Selector();

    const Vector<Component>& components() const { return m_components; }

    Specificity specificity() const;

private:
    Vector<Component> m_components;
};
