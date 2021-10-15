/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/Parser/Parser.h>

namespace Web::CSS {

CSSStyleRule::CSSStyleRule(NonnullRefPtrVector<Selector>&& selectors, NonnullRefPtr<CSSStyleDeclaration>&& declaration)
    : m_selectors(move(selectors))
    , m_declaration(move(declaration))
{
}

CSSStyleRule::~CSSStyleRule()
{
}

// https://www.w3.org/TR/cssom/#dom-cssstylerule-style
CSSStyleDeclaration* CSSStyleRule::style()
{
    return m_declaration;
}

// https://www.w3.org/TR/cssom/#serialize-a-css-rule
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

// https://www.w3.org/TR/cssom/#dom-cssstylerule-selectortext
String CSSStyleRule::selector_text() const
{
    // The selectorText attribute, on getting, must return the result of serializing the associated group of selectors.
    return serialize_a_group_of_selectors(selectors());
}

// https://www.w3.org/TR/cssom/#dom-cssstylerule-selectortext
void CSSStyleRule::set_selector_text(StringView selector_text)
{
    // 1. Run the parse a group of selectors algorithm on the given value.
    auto parsed_selectors = parse_selector({}, selector_text);

    // 2. If the algorithm returns a non-null value replace the associated group of selectors with the returned value.
    if (parsed_selectors.has_value())
        m_selectors = parsed_selectors.release_value();

    // 3. Otherwise, if the algorithm returns a null value, do nothing.
}

}
