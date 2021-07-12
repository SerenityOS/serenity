/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace Web::CSS {

class Selector : public RefCounted<Selector> {
public:
    struct SimpleSelector {
        enum class Type {
            Invalid,
            Universal,
            TagName,
            Id,
            Class,
            Attribute,
            PseudoClass,
            PseudoElement,
        };
        Type type { Type::Invalid };

        struct NthChildPattern {
            int step_size = 0;
            int offset = 0;

            static NthChildPattern parse(StringView const& args);
        };

        struct PseudoClass {
            enum class Type {
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
                Active,
            };
            Type type { Type::None };

            // FIXME: We don't need this field on every single SimpleSelector, but it's also annoying to malloc it somewhere.
            // Only used when "pseudo_class" is "NthChild" or "NthLastChild".
            NthChildPattern nth_child_pattern;

            // FIXME: This wants to be a Selector, rather than parsing it each time it is used.
            String not_selector {};
        };
        PseudoClass pseudo_class;

        enum class PseudoElement {
            None,
            Before,
            After,
            FirstLine,
            FirstLetter,
        };
        PseudoElement pseudo_element { PseudoElement::None };

        FlyString value;

        struct Attribute {
            enum class MatchType {
                None,
                HasAttribute,
                ExactValueMatch,
                ContainsWord,      // [att~=val]
                ContainsString,    // [att*=val]
                StartsWithSegment, // [att|=val]
                StartsWithString,  // [att^=val]
                EndsWithString,    // [att$=val]
            };
            MatchType match_type { MatchType::None };
            FlyString name;
            String value;
        };
        Attribute attribute;
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

    static NonnullRefPtr<Selector> create(Vector<ComplexSelector>&& complex_selectors)
    {
        return adopt_ref(*new Selector(move(complex_selectors)));
    }

    ~Selector();

    Vector<ComplexSelector> const& complex_selectors() const { return m_complex_selectors; }

    u32 specificity() const;

private:
    explicit Selector(Vector<ComplexSelector>&&);

    Vector<ComplexSelector> m_complex_selectors;
};

}
