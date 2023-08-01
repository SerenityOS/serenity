/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace Web::CSS {

using SelectorList = Vector<NonnullRefPtr<class Selector>>;

// This is a <complex-selector> in the spec. https://www.w3.org/TR/selectors-4/#complex
class Selector : public RefCounted<Selector> {
public:
    enum class PseudoElement {
        Before,
        After,
        FirstLine,
        FirstLetter,
        Marker,
        ProgressValue,
        ProgressBar,
        Placeholder,
        Selection,

        // Keep this last.
        PseudoElementCount,
    };

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
            ErrorOr<String> serialize() const
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
                    TRY(result.try_append('n'));
                // - A is -1: Append "-n" to result.
                else if (step_size == -1)
                    TRY(result.try_append("-n"sv));
                // - A is non-zero: Serialize A and append it to result, then append "n" to result.
                else if (step_size != 0)
                    TRY(result.try_appendff("{}n", step_size));

                // 4.
                // - B is greater than zero: Append "+" to result, then append the serialization of B to result.
                if (offset > 0)
                    TRY(result.try_appendff("+{}", offset));
                // - B is less than zero: Append the serialization of B to result.
                if (offset < 0)
                    TRY(result.try_appendff("{}", offset));

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
                Host,
                FirstOfType,
                LastOfType,
                OnlyOfType,
                NthOfType,
                NthLastOfType,
                Disabled,
                Enabled,
                Checked,
                Indeterminate,
                Is,
                Not,
                Where,
                Active,
                Lang,
                Scope,
                Defined,
                Playing,
                Paused,
                Seeking,
                Muted,
                VolumeLocked,
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

        struct Name {
            Name(FlyString n)
                : name(move(n))
                , lowercase_name(name.to_string().to_lowercase().release_value_but_fixme_should_propagate_errors())
            {
            }

            FlyString name;
            FlyString lowercase_name;
        };

        Type type;
        Variant<Empty, Attribute, PseudoClass, PseudoElement, Name> value {};

        Attribute const& attribute() const { return value.get<Attribute>(); }
        Attribute& attribute() { return value.get<Attribute>(); }
        PseudoClass const& pseudo_class() const { return value.get<PseudoClass>(); }
        PseudoClass& pseudo_class() { return value.get<PseudoClass>(); }
        PseudoElement const& pseudo_element() const { return value.get<PseudoElement>(); }
        PseudoElement& pseudo_element() { return value.get<PseudoElement>(); }

        FlyString const& name() const { return value.get<Name>().name; }
        FlyString& name() { return value.get<Name>().name; }
        FlyString const& lowercase_name() const { return value.get<Name>().lowercase_name; }
        FlyString& lowercase_name() { return value.get<Name>().lowercase_name; }

        ErrorOr<String> serialize() const;
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
    ErrorOr<String> serialize() const;

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
    case Selector::PseudoElement::ProgressBar:
        return "-webkit-progress-bar"sv;
    case Selector::PseudoElement::ProgressValue:
        return "-webkit-progress-value"sv;
    case Selector::PseudoElement::Placeholder:
        return "placeholder"sv;
    case Selector::PseudoElement::Selection:
        return "selection"sv;
    case Selector::PseudoElement::PseudoElementCount:
        break;
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
    case Selector::SimpleSelector::PseudoClass::Type::Host:
        return "host"sv;
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
    case Selector::SimpleSelector::PseudoClass::Type::Indeterminate:
        return "indeterminate"sv;
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
    case Selector::SimpleSelector::PseudoClass::Type::Scope:
        return "scope"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Defined:
        return "defined"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Playing:
        return "playing"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Paused:
        return "paused"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Seeking:
        return "seeking"sv;
    case Selector::SimpleSelector::PseudoClass::Type::Muted:
        return "muted"sv;
    case Selector::SimpleSelector::PseudoClass::Type::VolumeLocked:
        return "volume-locked"sv;
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<String> serialize_a_group_of_selectors(Vector<NonnullRefPtr<Selector>> const& selectors);

}

namespace AK {

template<>
struct Formatter<Web::CSS::Selector> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::Selector const& selector)
    {
        return Formatter<StringView>::format(builder, TRY(selector.serialize()));
    }
};

}
