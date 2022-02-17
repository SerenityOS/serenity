/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace Web::CSS {

using SelectorList = NonnullRefPtrVector<class Selector>;

// This is a <complex-selector> in the spec. https://www.w3.org/TR/selectors-4/#complex
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

        struct ANPlusBPattern {
            int step_size { 0 }; // "A"
            int offset = { 0 };  // "B"

            String serialize() const
            {
                return String::formatted("{}n{:+}", step_size, offset);
            }
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
                OnlyOfType,
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
            ANPlusBPattern nth_child_pattern;

            SelectorList not_selector {};
        };
        PseudoClass pseudo_class {};

        enum class PseudoElement {
            None,
            Before,
            After,
            FirstLine,
            FirstLetter,
        };
        PseudoElement pseudo_element { PseudoElement::None };

        FlyString value {};

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
            FlyString name {};
            String value {};
        };
        Attribute attribute {};

        String serialize() const;
    };

    enum class Combinator {
        None,
        ImmediateChild,    // >
        Descendant,        // <whitespace>
        NextSibling,       // +
        SubsequentSibling, // ~
        Column,            // ||
    };

    struct CompoundSelector {
        // Spec-wise, the <combinator> is not part of a <compound-selector>,
        // but it is more understandable to put them together.
        Combinator combinator { Combinator::None };
        Vector<SimpleSelector> simple_selectors;
    };

    static NonnullRefPtr<Selector> create(Vector<CompoundSelector>&& compound_selectors)
    {
        return adopt_ref(*new Selector(move(compound_selectors)));
    }

    ~Selector();

    Vector<CompoundSelector> const& compound_selectors() const { return m_compound_selectors; }

    u32 specificity() const;
    String serialize() const;

private:
    explicit Selector(Vector<CompoundSelector>&&);

    Vector<CompoundSelector> m_compound_selectors;
    mutable Optional<u32> m_specificity;
};

constexpr StringView pseudo_element_name(Selector::SimpleSelector::PseudoElement);
constexpr StringView pseudo_class_name(Selector::SimpleSelector::PseudoClass::Type);

String serialize_a_group_of_selectors(NonnullRefPtrVector<Selector> const& selectors);

}

namespace AK {

template<>
struct Formatter<Web::CSS::Selector> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::Selector const& selector)
    {
        return Formatter<StringView>::format(builder, selector.serialize());
    }
};

}
