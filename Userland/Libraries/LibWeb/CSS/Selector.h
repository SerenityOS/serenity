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
#include <LibWeb/CSS/PseudoClass.h>
#include <LibWeb/CSS/ValueID.h>

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
        MeterBar,
        MeterEvenLessGoodValue,
        MeterOptimumValue,
        MeterSuboptimumValue,
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
            String serialize() const
            {
                // 1. If A is zero, return the serialization of B.
                if (step_size == 0) {
                    return MUST(String::number(offset));
                }

                // 2. Otherwise, let result initially be an empty string.
                StringBuilder result;

                // 3.
                // - A is 1: Append "n" to result.
                if (step_size == 1)
                    result.append('n');
                // - A is -1: Append "-n" to result.
                else if (step_size == -1)
                    result.append("-n"sv);
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
                return MUST(result.to_string());
            }
        };

        struct PseudoClassSelector {
            PseudoClass type;

            // FIXME: We don't need this field on every single SimpleSelector, but it's also annoying to malloc it somewhere.
            // Only used when "pseudo_class" is "NthChild" or "NthLastChild".
            ANPlusBPattern nth_child_pattern {};

            SelectorList argument_selector_list {};

            // Used for :lang(en-gb,dk)
            Vector<FlyString> languages {};

            // Used by :dir()
            Optional<ValueID> identifier {};
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

        // Equivalent to `<wq-name>`
        // https://www.w3.org/TR/selectors-4/#typedef-wq-name
        struct QualifiedName {
            enum class NamespaceType {
                Default, // `E`
                None,    // `|E`
                Any,     // `*|E`
                Named,   // `ns|E`
            };
            NamespaceType namespace_type { NamespaceType::Default };
            FlyString namespace_ {};
            Name name;
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
            QualifiedName qualified_name;
            String value {};
            CaseType case_type;
        };

        Type type;
        Variant<Empty, Attribute, PseudoClassSelector, PseudoElement, Name, QualifiedName> value {};

        Attribute const& attribute() const { return value.get<Attribute>(); }
        Attribute& attribute() { return value.get<Attribute>(); }
        PseudoClassSelector const& pseudo_class() const { return value.get<PseudoClassSelector>(); }
        PseudoClassSelector& pseudo_class() { return value.get<PseudoClassSelector>(); }
        PseudoElement const& pseudo_element() const { return value.get<PseudoElement>(); }
        PseudoElement& pseudo_element() { return value.get<PseudoElement>(); }

        FlyString const& name() const { return value.get<Name>().name; }
        FlyString& name() { return value.get<Name>().name; }
        FlyString const& lowercase_name() const { return value.get<Name>().lowercase_name; }
        FlyString& lowercase_name() { return value.get<Name>().lowercase_name; }
        QualifiedName const& qualified_name() const { return value.get<QualifiedName>(); }
        QualifiedName& qualified_name() { return value.get<QualifiedName>(); }

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
    case Selector::PseudoElement::MeterBar:
        return "-webkit-meter-bar"sv;
    case Selector::PseudoElement::MeterEvenLessGoodValue:
        return "-webkit-meter-even-less-good-value"sv;
    case Selector::PseudoElement::MeterOptimumValue:
        return "-webkit-meter-optimum-value"sv;
    case Selector::PseudoElement::MeterSuboptimumValue:
        return "-webkit-meter-suboptimum-value"sv;
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

String serialize_a_group_of_selectors(Vector<NonnullRefPtr<Selector>> const& selectors);

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
