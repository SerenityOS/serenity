/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
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
    enum class PseudoElement {
        Before,
        After,
        FirstLine,
        FirstLetter,
        Marker,
    };
    static auto constexpr PseudoElementCount = to_underlying(PseudoElement::Marker) + 1;

    struct SimpleSelector {
        enum class Type {
            Universal,
            TagName,
            Id,
            Class,
            Attribute,
            PseudoClass,
            PseudoElement,
        };

        struct ANPlusBPattern {
            int step_size { 0 }; // "A"
            int offset = { 0 };  // "B"

            // https://www.w3.org/TR/css-syntax-3/#serializing-anb
            String serialize() const
            {
                // 1. If A is zero, return the serialization of B.
                if (step_size == 0) {
                    return String::formatted("{}", offset);
                }

                // 2. Otherwise, let result initially be an empty string.
                StringBuilder result;

                // 3.
                // - A is 1: Append "n" to result.
                if (step_size == 1)
                    result.append("n");
                // - A is -1: Append "-n" to result.
                else if (step_size == -1)
                    result.append("-n");
                // - A is non-zero: Serialize A and append it to result, then append "n" to result.
                else if (step_size != 0)
                    result.appendff("{}n", step_size);

                // 4.
                // - B is greater than zero: Append "+" to result, then append the serialization of B to result.
                if (offset > 0)
                    result.appendff("+{}", offset);
                // - B is less than zero: Append the serialization of B to result.
                if (offset < 0)
                    result.appendff("{}", offset);

                // 5. Return result.
                return result.to_string();
            }
        };

        struct PseudoClass {
            enum class Type {
                Link,
                Visited,
                Hover,
                Focus,
                FocusWithin,
                FirstChild,
                LastChild,
                OnlyChild,
                NthChild,
                NthLastChild,
                Empty,
                Root,
                FirstOfType,
                LastOfType,
                OnlyOfType,
                NthOfType,
                NthLastOfType,
                Disabled,
                Enabled,
                Checked,
                Is,
                Not,
                Where,
                Active,
                Lang,
            };
            Type type;

            // FIXME: We don't need this field on every single SimpleSelector, but it's also annoying to malloc it somewhere.
            // Only used when "pseudo_class" is "NthChild" or "NthLastChild".
            ANPlusBPattern nth_child_pattern {};

            SelectorList argument_selector_list {};

            // Used for :lang(en-gb,dk)
            Vector<FlyString> languages {};
        };

        struct Attribute {
            enum class MatchType {
                HasAttribute,
                ExactValueMatch,
                ContainsWord,      // [att~=val]
                ContainsString,    // [att*=val]
                StartsWithSegment, // [att|=val]
                StartsWithString,  // [att^=val]
                EndsWithString,    // [att$=val]
            };
            enum class CaseType {
                DefaultMatch,
                CaseSensitiveMatch,
                CaseInsensitiveMatch,
            };
            MatchType match_type;
            FlyString name {};
            String value {};
            CaseType case_type;
        };

        Type type;
        Variant<Empty, Attribute, PseudoClass, PseudoElement, FlyString> value {};

        Attribute const& attribute() const { return value.get<Attribute>(); }
        Attribute& attribute() { return value.get<Attribute>(); }
        PseudoClass const& pseudo_class() const { return value.get<PseudoClass>(); }
        PseudoClass& pseudo_class() { return value.get<PseudoClass>(); }
        PseudoElement const& pseudo_element() const { return value.get<PseudoElement>(); }
        PseudoElement& pseudo_element() { return value.get<PseudoElement>(); }
        FlyString const& name() const { return value.get<FlyString>(); }
        FlyString& name() { return value.get<FlyString>(); }

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

    ~Selector() = default;

    Vector<CompoundSelector> const& compound_selectors() const { return m_compound_selectors; }
    Optional<PseudoElement> pseudo_element() const { return m_pseudo_element; }
    u32 specificity() const;
    String serialize() const;

private:
    explicit Selector(Vector<CompoundSelector>&&);

    Vector<CompoundSelector> m_compound_selectors;
    mutable Optional<u32> m_specificity;
    Optional<Selector::PseudoElement> m_pseudo_element;
};

constexpr StringView pseudo_element_name(Selector::PseudoElement pseudo_element)
{
    switch (pseudo_element) {
    case Selector::PseudoElement::Before:
        return "before"sv;
    case Selector::PseudoElement::After:
        return "after"sv;
    case Selector::PseudoElement::FirstLine:
        return "first-line"sv;
    case Selector::PseudoElement::FirstLetter:
        return "first-letter"sv;
    case Selector::PseudoElement::Marker:
        return "marker"sv;
    }
    VERIFY_NOT_REACHED();
}

Optional<Selector::PseudoElement> pseudo_element_from_string(StringView);

constexpr StringView pseudo_class_name(Selector::SimpleSelector::PseudoClass::Type pseudo_class)
{
    switch (pseudo_class) {
    case Selector::SimpleSelector::PseudoClass::Type::Link:
        return "link"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Visited:
        return "visited"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Hover:
        return "hover"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Focus:
        return "focus"sv;
    case Selector::SimpleSelector::PseudoClass::Type::FocusWithin:
        return "focus-within"sv;
    case Selector::SimpleSelector::PseudoClass::Type::FirstChild:
        return "first-child"sv;
    case Selector::SimpleSelector::PseudoClass::Type::LastChild:
        return "last-child"sv;
    case Selector::SimpleSelector::PseudoClass::Type::OnlyChild:
        return "only-child"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Empty:
        return "empty"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Root:
        return "root"sv;
    case Selector::SimpleSelector::PseudoClass::Type::FirstOfType:
        return "first-of-type"sv;
    case Selector::SimpleSelector::PseudoClass::Type::LastOfType:
        return "last-of-type"sv;
    case Selector::SimpleSelector::PseudoClass::Type::OnlyOfType:
        return "only-of-type"sv;
    case Selector::SimpleSelector::PseudoClass::Type::NthOfType:
        return "nth-of-type"sv;
    case Selector::SimpleSelector::PseudoClass::Type::NthLastOfType:
        return "nth-last-of-type"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Disabled:
        return "disabled"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Enabled:
        return "enabled"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Checked:
        return "checked"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Active:
        return "active"sv;
    case Selector::SimpleSelector::PseudoClass::Type::NthChild:
        return "nth-child"sv;
    case Selector::SimpleSelector::PseudoClass::Type::NthLastChild:
        return "nth-last-child"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Is:
        return "is"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Not:
        return "not"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Where:
        return "where"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Lang:
        return "lang"sv;
    }
    VERIFY_NOT_REACHED();
}

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
