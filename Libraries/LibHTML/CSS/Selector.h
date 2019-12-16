#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibHTML/CSS/Specificity.h>

class Selector {
public:
    struct SimpleSelector {
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
            FirstChild,
            LastChild,
            OnlyChild,
            Empty,
        };
        PseudoClass pseudo_class { PseudoClass::None };

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

    struct ComplexSelector {
        enum class Relation {
            None,
            ImmediateChild,
            Descendant,
            AdjacentSibling,
            GeneralSibling,
        };
        Relation relation { Relation::None };

        using CompoundSelector = Vector<SimpleSelector>;
        CompoundSelector compound_selector;
    };

    explicit Selector(Vector<ComplexSelector>&&);
    ~Selector();

    const Vector<ComplexSelector>& complex_selectors() const { return m_complex_selectors; }

    Specificity specificity() const;

private:
    Vector<ComplexSelector> m_complex_selectors;
};
