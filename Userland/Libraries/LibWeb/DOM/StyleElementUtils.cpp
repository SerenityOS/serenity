/*
 * Copyright (c) 2023, Preston Taylor <PrestonLeeTaylor@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/StyleElementUtils.h>
#include <LibWeb/Infra/Strings.h>

namespace Web::DOM {

// The user agent must run the "update a style block" algorithm whenever one of the following conditions occur:
// FIXME: The element is popped off the stack of open elements of an HTML parser or XML parser.
//
// NOTE: This is basically done by children_changed() today:
// The element's children changed steps run.
//
// NOTE: This is basically done by inserted() and removed_from() today:
// The element is not on the stack of open elements of an HTML parser or XML parser, and it becomes connected or disconnected.
//
// https://html.spec.whatwg.org/multipage/semantics.html#update-a-style-block
void StyleElementUtils::update_a_style_block(DOM::Element& style_element)
{
    // OPTIMIZATION: Skip parsing CSS if we're in the middle of parsing a HTML fragment.
    //               The style block will be parsed upon insertion into a proper document.
    if (style_element.document().is_temporary_document_for_fragment_parsing())
        return;

    // 1. Let element be the style element.
    // 2. If element has an associated CSS style sheet, remove the CSS style sheet in question.

    if (m_associated_css_style_sheet) {
        remove_a_css_style_sheet(style_element.document(), *m_associated_css_style_sheet);

        // FIXME: This should probably be handled by StyleSheet::set_owner_node().
        m_associated_css_style_sheet = nullptr;
    }

    // 3. If element is not connected, then return.
    if (!style_element.is_connected())
        return;

    // 4. If element's type attribute is present and its value is neither the empty string nor an ASCII case-insensitive match for "text/css", then return.
    auto type_attribute = style_element.deprecated_attribute(HTML::AttributeNames::type);
    if (!type_attribute.is_null() && !type_attribute.is_empty() && !Infra::is_ascii_case_insensitive_match(type_attribute, "text/css"sv))
        return;

    // FIXME: 5. If the Should element's inline behavior be blocked by Content Security Policy? algorithm returns "Blocked" when executed upon the style element, "style", and the style element's child text content, then return. [CSP]

    // FIXME: This is a bit awkward, as the spec doesn't actually tell us when to parse the CSS text,
    //        so we just do it here and pass the parsed sheet to create_a_css_style_sheet().
    auto* sheet = parse_css_stylesheet(CSS::Parser::ParsingContext(style_element.document()), style_element.text_content().value_or(String {}));
    if (!sheet)
        return;

    // FIXME: This should probably be handled by StyleSheet::set_owner_node().
    m_associated_css_style_sheet = sheet;

    // 6. Create a CSS style sheet with the following properties...
    create_a_css_style_sheet(
        style_element.document(),
        "text/css"sv,
        &style_element,
        style_element.deprecated_attribute(HTML::AttributeNames::media),
        style_element.in_a_document_tree() ? style_element.deprecated_attribute(HTML::AttributeNames::title) : DeprecatedString::empty(),
        false,
        true,
        {},
        nullptr,
        nullptr,
        *sheet);
}

// https://www.w3.org/TR/cssom/#remove-a-css-style-sheet
void StyleElementUtils::remove_a_css_style_sheet(DOM::Document& document, CSS::CSSStyleSheet& sheet)
{
    // 1. Remove the CSS style sheet from the list of document or shadow root CSS style sheets.
    document.style_sheets().remove_sheet(sheet);

    // 2. Set the CSS style sheet’s parent CSS style sheet, owner node and owner CSS rule to null.
    sheet.set_parent_css_style_sheet(nullptr);
    sheet.set_owner_node(nullptr);
    sheet.set_owner_css_rule(nullptr);
}

// https://www.w3.org/TR/cssom/#create-a-css-style-sheet
void StyleElementUtils::create_a_css_style_sheet(DOM::Document& document, DeprecatedString type, DOM::Element* owner_node, DeprecatedString media, DeprecatedString title, bool alternate, bool origin_clean, DeprecatedString location, CSS::CSSStyleSheet* parent_style_sheet, CSS::CSSRule* owner_rule, CSS::CSSStyleSheet& sheet)
{
    // 1. Create a new CSS style sheet object and set its properties as specified.
    // FIXME: We receive `sheet` from the caller already. This is weird.

    sheet.set_parent_css_style_sheet(parent_style_sheet);
    sheet.set_owner_css_rule(owner_rule);
    sheet.set_owner_node(owner_node);
    sheet.set_type(MUST(String::from_deprecated_string(type)));
    sheet.set_media(move(media));
    if (title.is_null())
        sheet.set_title({});
    else
        sheet.set_title(MUST(String::from_deprecated_string(title)));
    sheet.set_alternate(alternate);
    sheet.set_origin_clean(origin_clean);
    if (location.is_null())
        sheet.set_location({});
    else
        sheet.set_location(MUST(String::from_deprecated_string(location)));

    // 2. Then run the add a CSS style sheet steps for the newly created CSS style sheet.
    add_a_css_style_sheet(document, sheet);
}

// https://www.w3.org/TR/cssom/#add-a-css-style-sheet
void StyleElementUtils::add_a_css_style_sheet(DOM::Document& document, CSS::CSSStyleSheet& sheet)
{
    // 1. Add the CSS style sheet to the list of document or shadow root CSS style sheets at the appropriate location. The remainder of these steps deal with the disabled flag.
    document.style_sheets().add_sheet(sheet);

    // 2. If the disabled flag is set, then return.
    if (sheet.disabled())
        return;

    // FIXME: 3. If the title is not the empty string, the alternate flag is unset, and preferred CSS style sheet set name is the empty string change the preferred CSS style sheet set name to the title.

    // FIXME: 4. If any of the following is true, then unset the disabled flag and return:
    //           The title is the empty string.
    //           The last CSS style sheet set name is null and the title is a case-sensitive match for the preferred CSS style sheet set name.
    //           The title is a case-sensitive match for the last CSS style sheet set name.

    // FIXME: 5. Set the disabled flag.
}

}
