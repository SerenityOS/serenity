/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Selector.h"
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS {

Selector::Selector(Vector<CompoundSelector>&& compound_selectors)
    : m_compound_selectors(move(compound_selectors))
{
}

Selector::~Selector()
{
}

u32 Selector::specificity() const
{
    if (m_specificity.has_value())
        return *m_specificity;

    unsigned ids = 0;
    unsigned tag_names = 0;
    unsigned classes = 0;

    for (auto& list : m_compound_selectors) {
        for (auto& simple_selector : list.simple_selectors) {
            switch (simple_selector.type) {
            case SimpleSelector::Type::Id:
                ++ids;
                break;
            case SimpleSelector::Type::Class:
                ++classes;
                break;
            case SimpleSelector::Type::TagName:
                ++tag_names;
                break;
            default:
                break;
            }
        }
    }

    m_specificity = ids * 0x10000 + classes * 0x100 + tag_names;

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
            serialize_an_identifier(s, value);
        }
        // 4. If this is a universal selector append "*" (U+002A) to s.
        if (type == Selector::SimpleSelector::Type::Universal)
            s.append('*');
        break;
    case Selector::SimpleSelector::Type::Attribute:
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
        // FIXME: 5. If the attribute selector has the case-sensitivity flag present, append " i" (U+0020 U+0069) to s.

        // 6. Append "]" (U+005D) to s.
        s.append(']');
        break;

    case Selector::SimpleSelector::Type::Class:
        // Append a "." (U+002E), followed by the serialization of the class name as an identifier to s.
        s.append('.');
        serialize_an_identifier(s, value);
        break;

    case Selector::SimpleSelector::Type::Id:
        // Append a "#" (U+0023), followed by the serialization of the ID as an identifier to s.
        s.append('#');
        serialize_an_identifier(s, value);
        break;

    case Selector::SimpleSelector::Type::PseudoClass:
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
            // Otherwise, append ":" (U+003A), followed by the name of the pseudo-class, followed by "(" (U+0028),
            // followed by the value of the pseudo-class argument(s) determined as per below, followed by ")" (U+0029), to s.
            s.append(':');
            s.append(pseudo_class_name(pseudo_class.type));
            s.append('(');
            if (pseudo_class.type == Selector::SimpleSelector::PseudoClass::Type::NthChild
                || pseudo_class.type == Selector::SimpleSelector::PseudoClass::Type::NthLastChild) {
                // The result of serializing the value using the rules to serialize an <an+b> value.
                s.append(pseudo_class.nth_child_pattern.serialize());
            } else if (pseudo_class.type == Selector::SimpleSelector::PseudoClass::Type::Not) {
                // The result of serializing the value using the rules for serializing a group of selectors.
                s.append(serialize_a_group_of_selectors(pseudo_class.not_selector));
            }
            s.append(')');
            break;
        default:
            VERIFY_NOT_REACHED();
        }
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
            s.append(compound_selectors().first().simple_selectors.first().serialize());
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
                s.append(pseudo_element_name(compound_selector.simple_selectors.last().pseudo_element));
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

constexpr StringView pseudo_element_name(Selector::SimpleSelector::PseudoElement pseudo_element)
{
    switch (pseudo_element) {
    case Selector::SimpleSelector::PseudoElement::Before:
        return "before"sv;
    case Selector::SimpleSelector::PseudoElement::After:
        return "after"sv;
    case Selector::SimpleSelector::PseudoElement::FirstLine:
        return "first-line"sv;
    case Selector::SimpleSelector::PseudoElement::FirstLetter:
        return "first-letter"sv;
    case Selector::SimpleSelector::PseudoElement::None:
        break;
    }
    VERIFY_NOT_REACHED();
}

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
    case Selector::SimpleSelector::PseudoClass::Type::Not:
        return "not"sv;
    case Selector::SimpleSelector::PseudoClass::Type::None:
        break;
    }
    VERIFY_NOT_REACHED();
}

}
