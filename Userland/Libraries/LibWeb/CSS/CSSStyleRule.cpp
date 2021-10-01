/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSStyleRule.h>

namespace Web::CSS {

CSSStyleRule::CSSStyleRule(NonnullRefPtrVector<Selector>&& selectors, NonnullRefPtr<CSSStyleDeclaration>&& declaration)
    : m_selectors(move(selectors))
    , m_declaration(move(declaration))
{
}

CSSStyleRule::~CSSStyleRule()
{
}

// https://drafts.csswg.org/cssom/#dom-cssstylerule-style
CSSStyleDeclaration* CSSStyleRule::style()
{
    return m_declaration;
}

static StringView to_string(Selector::SimpleSelector::PseudoElement pseudo_element)
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

static StringView to_string(Selector::SimpleSelector::PseudoClass::Type pseudo_class)
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
        return "first-of-pseudo_class"sv;
    case Selector::SimpleSelector::PseudoClass::Type::LastOfType:
        return "last-of-pseudo_class"sv;
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

static String serialize_a_group_of_selectors(NonnullRefPtrVector<Selector> const&);

// https://drafts.csswg.org/cssom/#serialize-a-simple-selector
static String serialize_a_simple_selector(Selector::SimpleSelector const& simple_selector)
{
    StringBuilder builder;
    switch (simple_selector.type) {
    case Selector::SimpleSelector::Type::TagName:
    case Selector::SimpleSelector::Type::Universal:
        // FIXME: 1. If the namespace prefix maps to a namespace that is not the default namespace and is not the null namespace (not in a namespace) append the serialization of the namespace prefix as an identifier, followed by a "|" (U+007C) to s.
        // FIXME: 2. If the namespace prefix maps to a namespace that is the null namespace (not in a namespace) append "|" (U+007C) to s.
        // 3. If this is a type selector append the serialization of the element name as an identifier to s.
        if (simple_selector.type == Selector::SimpleSelector::Type::TagName) {
            // FIXME: Use the "serialize an identifier" algorithm.
            builder.append(simple_selector.value);
        }
        // 4. If this is a universal selector append "*" (U+002A) to s.
        if (simple_selector.type == Selector::SimpleSelector::Type::Universal)
            builder.append('*');
        break;
    case Selector::SimpleSelector::Type::Attribute:
        // 1. Append "[" (U+005B) to s.
        builder.append('[');

        // FIXME: 2. If the namespace prefix maps to a namespace that is not the null namespace (not in a namespace) append the serialization of the namespace prefix as an identifier, followed by a "|" (U+007C) to s.

        // 3. Append the serialization of the attribute name as an identifier to s.
        // FIXME: Use the "serialize an identifier" algorithm.
        builder.append(simple_selector.attribute.name);

        // 4. If there is an attribute value specified, append "=", "~=", "|=", "^=", "$=", or "*=" as appropriate (depending on the type of attribute selector),
        //    followed by the serialization of the attribute value as a string, to s.
        if (!simple_selector.attribute.value.is_null()) {
            switch (simple_selector.attribute.match_type) {
            case Selector::SimpleSelector::Attribute::MatchType::ExactValueMatch:
                builder.append("=");
                break;
            case Selector::SimpleSelector::Attribute::MatchType::ContainsWord:
                builder.append("~=");
                break;
            case Selector::SimpleSelector::Attribute::MatchType::ContainsString:
                builder.append("*=");
                break;
            case Selector::SimpleSelector::Attribute::MatchType::StartsWithSegment:
                builder.append("|=");
                break;
            case Selector::SimpleSelector::Attribute::MatchType::StartsWithString:
                builder.append("^=");
                break;
            case Selector::SimpleSelector::Attribute::MatchType::EndsWithString:
                builder.append("$=");
                break;
            default:
                break;
            }
        }
        // FIXME: 5. If the attribute selector has the case-sensitivity flag present, append " i" (U+0020 U+0069) to s.

        // 6. Append "]" (U+005D) to s.
        builder.append(']');
        break;

    case Selector::SimpleSelector::Type::Class:
        // Append a "." (U+002E), followed by the serialization of the class name as an identifier to s.
        builder.append('.');
        // FIXME: Use the "serialize an identifier" algorithm.
        builder.append(simple_selector.value);
        break;

    case Selector::SimpleSelector::Type::Id:
        // Append a "#" (U+0023), followed by the serialization of the ID as an identifier to s.
        builder.append('#');
        // FIXME: Use the "serialize an identifier" algorithm.
        builder.append(simple_selector.value);
        break;

    case Selector::SimpleSelector::Type::PseudoClass:
        switch (simple_selector.pseudo_class.type) {
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
        case Selector::SimpleSelector::PseudoClass::Type::Disabled:
        case Selector::SimpleSelector::PseudoClass::Type::Enabled:
        case Selector::SimpleSelector::PseudoClass::Type::Checked:
        case Selector::SimpleSelector::PseudoClass::Type::Active:
            // If the pseudo-class does not accept arguments append ":" (U+003A), followed by the name of the pseudo-class, to s.
            builder.append(':');
            builder.append(to_string(simple_selector.pseudo_class.type));
            break;
        case Selector::SimpleSelector::PseudoClass::Type::NthChild:
        case Selector::SimpleSelector::PseudoClass::Type::NthLastChild:
        case Selector::SimpleSelector::PseudoClass::Type::Not:
            // Otherwise, append ":" (U+003A), followed by the name of the pseudo-class, followed by "(" (U+0028),
            // followed by the value of the pseudo-class argument(s) determined as per below, followed by ")" (U+0029), to s.
            builder.append(':');
            builder.append(to_string(simple_selector.pseudo_class.type));
            builder.append('(');
            if (simple_selector.pseudo_class.type == Selector::SimpleSelector::PseudoClass::Type::NthChild
                || simple_selector.pseudo_class.type == Selector::SimpleSelector::PseudoClass::Type::NthLastChild) {
                // FIXME: The result of serializing the value using the rules to serialize an <an+b> value.
                TODO();
            } else if (simple_selector.pseudo_class.type == Selector::SimpleSelector::PseudoClass::Type::Not) {
                // The result of serializing the value using the rules for serializing a group of selectors.
                builder.append(serialize_a_group_of_selectors(simple_selector.pseudo_class.not_selector));
            }
            builder.append(')');
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        break;
    default:
        dbgln("FIXME: Unsupported simple selector serialization for type {}", to_underlying(simple_selector.type));
        break;
    }
    return builder.to_string();
}

// https://drafts.csswg.org/cssom/#serialize-a-selector
static String serialize_a_selector(Selector const& selector)
{
    StringBuilder builder;

    // To serialize a selector let s be the empty string, run the steps below for each part of the chain of the selector, and finally return s:
    for (size_t i = 0; i < selector.compound_selectors().size(); ++i) {
        auto const& compound_selector = selector.compound_selectors()[i];
        // 1. If there is only one simple selector in the compound selectors which is a universal selector, append the result of serializing the universal selector to s.
        if (compound_selector.simple_selectors.size() == 1
            && compound_selector.simple_selectors.first().type == Selector::SimpleSelector::Type::Universal) {
            builder.append(serialize_a_simple_selector(selector.compound_selectors().first().simple_selectors.first()));
        }
        // 2. Otherwise, for each simple selector in the compound selectors...
        //    FIXME: ...that is not a universal selector of which the namespace prefix maps to a namespace that is not the default namespace...
        //    ...serialize the simple selector and append the result to s.
        else {
            for (auto& simple_selector : compound_selector.simple_selectors) {
                builder.append(serialize_a_simple_selector(simple_selector));
            }
        }

        // 3. If this is not the last part of the chain of the selector append a single SPACE (U+0020),
        //    followed by the combinator ">", "+", "~", ">>", "||", as appropriate, followed by another
        //    single SPACE (U+0020) if the combinator was not whitespace, to s.
        if (i != selector.compound_selectors().size() - 1) {
            builder.append(' ');
            switch (compound_selector.combinator) {
            case Selector::Combinator::ImmediateChild:
                builder.append('>');
                break;
            case Selector::Combinator::NextSibling:
                builder.append('+');
                break;
            case Selector::Combinator::SubsequentSibling:
                builder.append('~');
                break;
            case Selector::Combinator::Column:
                builder.append("||");
                break;
            default:
                break;
            }
        } else {
            // 4. If this is the last part of the chain of the selector and there is a pseudo-element, append "::" followed by the name of the pseudo-element, to s.
            // FIXME: This doesn't feel entirely correct. Our model of pseudo-elements seems off.
            if (!compound_selector.simple_selectors.is_empty()
                && compound_selector.simple_selectors.first().type == Selector::SimpleSelector::Type::PseudoElement) {
                builder.append("::");
                builder.append(to_string(compound_selector.simple_selectors.first().pseudo_element));
            }
        }
    }

    return builder.to_string();
}

// https://drafts.csswg.org/cssom/#serialize-a-group-of-selectors
static String serialize_a_group_of_selectors(NonnullRefPtrVector<Selector> const& selectors)
{
    // To serialize a group of selectors serialize each selector in the group of selectors and then serialize a comma-separated list of these serializations.
    StringBuilder builder;
    for (auto& selector : selectors)
        builder.append(serialize_a_selector(selector));
    return builder.to_string();
}

// https://drafts.csswg.org/cssom/#serialize-a-css-rule
String CSSStyleRule::serialized() const
{
    StringBuilder builder;

    // 1. Let s initially be the result of performing serialize a group of selectors on the rule’s associated selectors,
    //    followed by the string " {", i.e., a single SPACE (U+0020), followed by LEFT CURLY BRACKET (U+007B).
    builder.append(serialize_a_group_of_selectors(selectors()));
    builder.append(" {"sv);

    // 2. Let decls be the result of performing serialize a CSS declaration block on the rule’s associated declarations, or null if there are no such declarations.
    auto decls = declaration().serialized();

    // FIXME: 3. Let rules be the result of performing serialize a CSS rule on each rule in the rule’s cssRules list, or null if there are no such rules.
    String rules;

    // 4. If decls and rules are both null, append " }" to s (i.e. a single SPACE (U+0020) followed by RIGHT CURLY BRACKET (U+007D)) and return s.
    if (decls.is_null() && rules.is_null()) {
        builder.append(" }"sv);
        return builder.to_string();
    }

    // 5. If rules is null:
    if (rules.is_null()) {
        //    1. Append a single SPACE (U+0020) to s
        builder.append(' ');
        //    2. Append decls to s
        builder.append(decls);
        //    3. Append " }" to s (i.e. a single SPACE (U+0020) followed by RIGHT CURLY BRACKET (U+007D)).
        builder.append(" }"sv);
        //    4. Return s.
        return builder.to_string();
    }

    // FIXME: 6. Otherwise:
    // FIXME:    1. If decls is not null, prepend it to rules.
    // FIXME:    2. For each rule in rules:
    // FIXME:       1. Append a newline followed by two spaces to s.
    // FIXME:       2. Append rule to s.
    // FIXME:    3. Append a newline followed by RIGHT CURLY BRACKET (U+007D) to s.
    // FIXME:    4. Return s.
    TODO();
}

// https://drafts.csswg.org/cssom/#dom-cssstylerule-selectortext
String CSSStyleRule::selector_text() const
{
    // The selectorText attribute, on getting, must return the result of serializing the associated group of selectors.
    return serialized();
}

// https://drafts.csswg.org/cssom/#dom-cssstylerule-selectortext
void CSSStyleRule::set_selector_text(StringView selector_text)
{
    // FIXME: 1. Run the parse a group of selectors algorithm on the given value.

    // FIXME: 2. If the algorithm returns a non-null value replace the associated group of selectors with the returned value.

    // FIXME: 3. Otherwise, if the algorithm returns a null value, do nothing.

    (void)selector_text;
    TODO();
}

}
