/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace Web::CSS {

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
            Visited,
            Hover,
            Focus,
            FirstChild,
            LastChild,
            OnlyChild,
            Empty,
            Root,
            FirstOfType,
            LastOfType,
            NthChild,
            NthLastChild,
            Disabled,
            Enabled,
            Checked,
            Not,
        };
        PseudoClass pseudo_class { PseudoClass::None };

        enum class PseudoElement {
            None,
            Before,
            After,
        };
        PseudoElement pseudo_element { PseudoElement::None };

        FlyString value;

        enum class AttributeMatchType {
            None,
            HasAttribute,
            ExactValueMatch,
            Contains,
            StartsWith,
        };

        AttributeMatchType attribute_match_type { AttributeMatchType::None };
        FlyString attribute_name;
        String attribute_value;

        struct NthChildPattern {
            int step_size = 0;
            int offset = 0;

            static NthChildPattern parse(const StringView& args);
        };

        // FIXME: We don't need this field on every single SimpleSelector, but it's also annoying to malloc it somewhere.
        // Only used when "pseudo_class" is "NthChild" or "NthLastChild".
        NthChildPattern nth_child_pattern;
        String not_selector {};
    };

    struct ComplexSelector {
        enum class Relation {
            None,
            ImmediateChild,
            Descendant,
            AdjacentSibling,
            GeneralSibling,
            Column,
        };
        Relation relation { Relation::None };

        using CompoundSelector = Vector<SimpleSelector>;
        CompoundSelector compound_selector;
    };

    explicit Selector(Vector<ComplexSelector>&&);
    ~Selector();

    const Vector<ComplexSelector>& complex_selectors() const { return m_complex_selectors; }

    u32 specificity() const;

private:
    Vector<ComplexSelector> m_complex_selectors;
};

}
