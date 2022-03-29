/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Selector.h"
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS {

Selector::Selector(Vector<CompoundSelector>&& compound_selectors)
    : m_compound_selectors(move(compound_selectors))
{
    // FIXME: This assumes that only one pseudo-element is allowed in a selector, and that it appears at the end.
    //        This is not true in Selectors-4!
    if (!m_compound_selectors.is_empty()) {
        for (auto const& simple_selector : m_compound_selectors.last().simple_selectors) {
            if (simple_selector.type == SimpleSelector::Type::PseudoElement) {
                m_pseudo_element = simple_selector.pseudo_element();
                break;
            }
        }
    }
}

// https://www.w3.org/TR/selectors-4/#specificity-rules
u32 Selector::specificity() const
{
    if (m_specificity.has_value())
        return *m_specificity;

    constexpr u32 ids_shift = 16;
    constexpr u32 classes_shift = 8;
    constexpr u32 tag_names_shift = 0;
    constexpr u32 ids_mask = 0xff << ids_shift;
    constexpr u32 classes_mask = 0xff << classes_shift;
    constexpr u32 tag_names_mask = 0xff << tag_names_shift;

    u32 ids = 0;
    u32 classes = 0;
    u32 tag_names = 0;

    auto count_specificity_of_most_complex_selector = [&](auto& selector_list) {
        u32 max_selector_list_argument_specificity = 0;
        for (auto const& complex_selector : selector_list) {
            max_selector_list_argument_specificity = max(max_selector_list_argument_specificity, complex_selector.specificity());
        }

        u32 child_ids = (max_selector_list_argument_specificity & ids_mask) >> ids_shift;
        u32 child_classes = (max_selector_list_argument_specificity & classes_mask) >> classes_shift;
        u32 child_tag_names = (max_selector_list_argument_specificity & tag_names_mask) >> tag_names_shift;

        ids += child_ids;
        classes += child_classes;
        tag_names += child_tag_names;
    };

    for (auto& list : m_compound_selectors) {
        for (auto& simple_selector : list.simple_selectors) {
            switch (simple_selector.type) {
            case SimpleSelector::Type::Id:
                // count the number of ID selectors in the selector (= A)
                ++ids;
                break;
            case SimpleSelector::Type::Class:
            case SimpleSelector::Type::Attribute:
                // count the number of class selectors, attributes selectors, and pseudo-classes in the selector (= B)
                ++classes;
                break;
            case SimpleSelector::Type::PseudoClass: {
                auto& pseudo_class = simple_selector.pseudo_class();
                switch (pseudo_class.type) {
                case SimpleSelector::PseudoClass::Type::Is:
                case SimpleSelector::PseudoClass::Type::Not: {
                    // The specificity of an :is(), :not(), or :has() pseudo-class is replaced by the
                    // specificity of the most specific complex selector in its selector list argument.
                    count_specificity_of_most_complex_selector(pseudo_class.argument_selector_list);
                    break;
                }
                case SimpleSelector::PseudoClass::Type::NthChild:
                case SimpleSelector::PseudoClass::Type::NthLastChild: {
                    // Analogously, the specificity of an :nth-child() or :nth-last-child() selector
                    // is the specificity of the pseudo class itself (counting as one pseudo-class selector)
                    // plus the specificity of the most specific complex selector in its selector list argument (if any).
                    ++classes;
                    count_specificity_of_most_complex_selector(pseudo_class.argument_selector_list);
                    break;
                }
                case SimpleSelector::PseudoClass::Type::Where:
                    // The specificity of a :where() pseudo-class is replaced by zero.
                    break;
                default:
                    ++classes;
                    break;
                }
                break;
            }
            case SimpleSelector::Type::TagName:
            case SimpleSelector::Type::PseudoElement:
                // count the number of type selectors and pseudo-elements in the selector (= C)
                ++tag_names;
                break;
            case SimpleSelector::Type::Universal:
                // ignore the universal selector
                break;
            }
        }
    }

    // Due to storage limitations, implementations may have limitations on the size of A, B, or C.
    // If so, values higher than the limit must be clamped to that limit, and not overflow.
    m_specificity = (min(ids, 0xff) << ids_shift)
        + (min(classes, 0xff) << classes_shift)
        + (min(tag_names, 0xff) << tag_names_shift);

    return *m_specificity;
}

// https://www.w3.org/TR/cssom/#serialize-a-simple-selector
String Selector::SimpleSelector::serialize() const
{
    StringBuilder s;
    switch (type) {
    case Selector::SimpleSelector::Type::TagName:
    case Selector::SimpleSelector::Type::Universal:
        // FIXME: 1. If the namespace prefix maps to a namespace that is not the default namespace and is not the null namespace (not in a namespace) append the serialization of the namespace prefix as an identifier, followed by a "|" (U+007C) to s.
        // FIXME: 2. If the namespace prefix maps to a namespace that is the null namespace (not in a namespace) append "|" (U+007C) to s.
        // 3. If this is a type selector append the serialization of the element name as an identifier to s.
        if (type == Selector::SimpleSelector::Type::TagName) {
            serialize_an_identifier(s, name());
        }
        // 4. If this is a universal selector append "*" (U+002A) to s.
        if (type == Selector::SimpleSelector::Type::Universal)
            s.append('*');
        break;
    case Selector::SimpleSelector::Type::Attribute: {
        auto& attribute = this->attribute();

        // 1. Append "[" (U+005B) to s.
        s.append('[');

        // FIXME: 2. If the namespace prefix maps to a namespace that is not the null namespace (not in a namespace) append the serialization of the namespace prefix as an identifier, followed by a "|" (U+007C) to s.

        // 3. Append the serialization of the attribute name as an identifier to s.
        serialize_an_identifier(s, attribute.name);

        // 4. If there is an attribute value specified, append "=", "~=", "|=", "^=", "$=", or "*=" as appropriate (depending on the type of attribute selector),
        //    followed by the serialization of the attribute value as a string, to s.
        if (!attribute.value.is_null()) {
            switch (attribute.match_type) {
            case Selector::SimpleSelector::Attribute::MatchType::ExactValueMatch:
                s.append("=");
                break;
            case Selector::SimpleSelector::Attribute::MatchType::ContainsWord:
                s.append("~=");
                break;
            case Selector::SimpleSelector::Attribute::MatchType::ContainsString:
                s.append("*=");
                break;
            case Selector::SimpleSelector::Attribute::MatchType::StartsWithSegment:
                s.append("|=");
                break;
            case Selector::SimpleSelector::Attribute::MatchType::StartsWithString:
                s.append("^=");
                break;
            case Selector::SimpleSelector::Attribute::MatchType::EndsWithString:
                s.append("$=");
                break;
            default:
                break;
            }

            serialize_a_string(s, attribute.value);
        }

        // 5. If the attribute selector has the case-insensitivity flag present, append " i" (U+0020 U+0069) to s.
        //    If the attribute selector has the case-insensitivity flag present, append " s" (U+0020 U+0073) to s.
        //    (the line just above is an addition to CSS OM to match Selectors Level 4 last draft)
        switch (attribute.case_type) {
        case Selector::SimpleSelector::Attribute::CaseType::CaseInsensitiveMatch:
            s.append(" i");
            break;
        case Selector::SimpleSelector::Attribute::CaseType::CaseSensitiveMatch:
            s.append(" s");
            break;
        default:
            break;
        }

        // 6. Append "]" (U+005D) to s.
        s.append(']');
        break;
    }

    case Selector::SimpleSelector::Type::Class:
        // Append a "." (U+002E), followed by the serialization of the class name as an identifier to s.
        s.append('.');
        serialize_an_identifier(s, name());
        break;

    case Selector::SimpleSelector::Type::Id:
        // Append a "#" (U+0023), followed by the serialization of the ID as an identifier to s.
        s.append('#');
        serialize_an_identifier(s, name());
        break;

    case Selector::SimpleSelector::Type::PseudoClass: {
        auto& pseudo_class = this->pseudo_class();

        switch (pseudo_class.type) {
        case Selector::SimpleSelector::PseudoClass::Type::Link:
        case Selector::SimpleSelector::PseudoClass::Type::Visited:
        case Selector::SimpleSelector::PseudoClass::Type::Hover:
        case Selector::SimpleSelector::PseudoClass::Type::Focus:
        case Selector::SimpleSelector::PseudoClass::Type::FirstChild:
        case Selector::SimpleSelector::PseudoClass::Type::LastChild:
        case Selector::SimpleSelector::PseudoClass::Type::OnlyChild:
        case Selector::SimpleSelector::PseudoClass::Type::Empty:
        case Selector::SimpleSelector::PseudoClass::Type::Root:
        case Selector::SimpleSelector::PseudoClass::Type::FirstOfType:
        case Selector::SimpleSelector::PseudoClass::Type::LastOfType:
        case Selector::SimpleSelector::PseudoClass::Type::OnlyOfType:
        case Selector::SimpleSelector::PseudoClass::Type::Disabled:
        case Selector::SimpleSelector::PseudoClass::Type::Enabled:
        case Selector::SimpleSelector::PseudoClass::Type::Checked:
        case Selector::SimpleSelector::PseudoClass::Type::Active:
            // If the pseudo-class does not accept arguments append ":" (U+003A), followed by the name of the pseudo-class, to s.
            s.append(':');
            s.append(pseudo_class_name(pseudo_class.type));
            break;
        case Selector::SimpleSelector::PseudoClass::Type::NthChild:
        case Selector::SimpleSelector::PseudoClass::Type::NthLastChild:
        case Selector::SimpleSelector::PseudoClass::Type::Not:
        case Selector::SimpleSelector::PseudoClass::Type::Is:
        case Selector::SimpleSelector::PseudoClass::Type::Where:
            // Otherwise, append ":" (U+003A), followed by the name of the pseudo-class, followed by "(" (U+0028),
            // followed by the value of the pseudo-class argument(s) determined as per below, followed by ")" (U+0029), to s.
            s.append(':');
            s.append(pseudo_class_name(pseudo_class.type));
            s.append('(');
            if (pseudo_class.type == Selector::SimpleSelector::PseudoClass::Type::NthChild
                || pseudo_class.type == Selector::SimpleSelector::PseudoClass::Type::NthLastChild) {
                // The result of serializing the value using the rules to serialize an <an+b> value.
                s.append(pseudo_class.nth_child_pattern.serialize());
            } else if (pseudo_class.type == Selector::SimpleSelector::PseudoClass::Type::Not
                || pseudo_class.type == Selector::SimpleSelector::PseudoClass::Type::Is
                || pseudo_class.type == Selector::SimpleSelector::PseudoClass::Type::Where) {
                // The result of serializing the value using the rules for serializing a group of selectors.
                // NOTE: `:is()` and `:where()` aren't in the spec for this yet, but it should be!
                s.append(serialize_a_group_of_selectors(pseudo_class.argument_selector_list));
            }
            s.append(')');
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        break;
    }
    case Selector::SimpleSelector::Type::PseudoElement:
        // Note: Pseudo-elements are dealt with in Selector::serialize()
        break;
    default:
        dbgln("FIXME: Unsupported simple selector serialization for type {}", to_underlying(type));
        break;
    }
    return s.to_string();
}

// https://www.w3.org/TR/cssom/#serialize-a-selector
String Selector::serialize() const
{
    StringBuilder s;

    // To serialize a selector let s be the empty string, run the steps below for each part of the chain of the selector, and finally return s:
    for (size_t i = 0; i < compound_selectors().size(); ++i) {
        auto const& compound_selector = compound_selectors()[i];
        // 1. If there is only one simple selector in the compound selectors which is a universal selector, append the result of serializing the universal selector to s.
        if (compound_selector.simple_selectors.size() == 1
            && compound_selector.simple_selectors.first().type == Selector::SimpleSelector::Type::Universal) {
            s.append(compound_selector.simple_selectors.first().serialize());
        }
        // 2. Otherwise, for each simple selector in the compound selectors...
        //    FIXME: ...that is not a universal selector of which the namespace prefix maps to a namespace that is not the default namespace...
        //    ...serialize the simple selector and append the result to s.
        else {
            for (auto& simple_selector : compound_selector.simple_selectors) {
                s.append(simple_selector.serialize());
            }
        }

        // 3. If this is not the last part of the chain of the selector append a single SPACE (U+0020),
        //    followed by the combinator ">", "+", "~", ">>", "||", as appropriate, followed by another
        //    single SPACE (U+0020) if the combinator was not whitespace, to s.
        if (i != compound_selectors().size() - 1) {
            s.append(' ');
            // Note: The combinator that appears between parts `i` and `i+1` appears with the `i+1` selector,
            //       so we have to check that one.
            switch (compound_selectors()[i + 1].combinator) {
            case Selector::Combinator::ImmediateChild:
                s.append("> ");
                break;
            case Selector::Combinator::NextSibling:
                s.append("+ ");
                break;
            case Selector::Combinator::SubsequentSibling:
                s.append("~ ");
                break;
            case Selector::Combinator::Column:
                s.append("|| ");
                break;
            default:
                break;
            }
        } else {
            // 4. If this is the last part of the chain of the selector and there is a pseudo-element,
            // append "::" followed by the name of the pseudo-element, to s.
            if (compound_selector.simple_selectors.last().type == Selector::SimpleSelector::Type::PseudoElement) {
                s.append("::");
                s.append(pseudo_element_name(compound_selector.simple_selectors.last().pseudo_element()));
            }
        }
    }

    return s.to_string();
}

// https://www.w3.org/TR/cssom/#serialize-a-group-of-selectors
String serialize_a_group_of_selectors(NonnullRefPtrVector<Selector> const& selectors)
{
    // To serialize a group of selectors serialize each selector in the group of selectors and then serialize a comma-separated list of these serializations.
    StringBuilder builder;
    builder.join(", ", selectors);
    return builder.to_string();
}

Optional<Selector::PseudoElement> pseudo_element_from_string(StringView name)
{
    if (name.equals_ignoring_case("after")) {
        return Selector::PseudoElement::After;
    } else if (name.equals_ignoring_case("before")) {
        return Selector::PseudoElement::Before;
    } else if (name.equals_ignoring_case("first-letter")) {
        return Selector::PseudoElement::FirstLetter;
    } else if (name.equals_ignoring_case("first-line")) {
        return Selector::PseudoElement::FirstLine;
    } else if (name.equals_ignoring_case("marker")) {
        return Selector::PseudoElement::Marker;
    }
    return {};
}

}
