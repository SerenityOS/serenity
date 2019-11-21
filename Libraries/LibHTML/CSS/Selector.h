#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibHTML/CSS/Specificity.h>

class Selector {
public:
    struct Component {
        enum class Type {
            Invalid,
            Universal,
            TagName,
            Id,
            Class,
        };
        Type type { Type::Invalid };

        enum class PseudoClass {
            None,
            Link,
            Hover,
        };
        PseudoClass pseudo_class { PseudoClass::None };

        enum class Relation {
            None,
            ImmediateChild,
            Descendant,
            AdjacentSibling,
            GeneralSibling,
        };
        Relation relation { Relation::None };

        String value;

        enum class AttributeMatchType {
            None,
            HasAttribute,
            ExactValueMatch,
        };

        AttributeMatchType attribute_match_type { AttributeMatchType::None };
        String attribute_name;
        String attribute_value;
    };

    explicit Selector(Vector<Component>&&);
    ~Selector();

    const Vector<Component>& components() const { return m_components; }

    Specificity specificity() const;

private:
    Vector<Component> m_components;
};
