/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLStyleElement.h>

namespace Web::HTML {

HTMLStyleElement::HTMLStyleElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLStyleElement::~HTMLStyleElement()
{
}

void HTMLStyleElement::children_changed()
{

    update_a_style_block();
    HTMLElement::children_changed();
}

void HTMLStyleElement::inserted()
{
    update_a_style_block();
    return HTMLElement::inserted();
}

void HTMLStyleElement::removed_from(Node* old_parent)
{
    update_a_style_block();
    return HTMLElement::removed_from(old_parent);
}

// https://www.w3.org/TR/cssom/#remove-a-css-style-sheet
static void remove_a_css_style_sheet(DOM::Document& document, NonnullRefPtr<CSS::CSSStyleSheet> sheet)
{
    VERIFY(sheet.ptr());

    // 1. Remove the CSS style sheet from the list of document or shadow root CSS style sheets.
    document.style_sheets().remove_sheet(sheet);

    // 2. Set the CSS style sheet’s parent CSS style sheet, owner node and owner CSS rule to null.
    sheet->set_parent_css_style_sheet(nullptr);
    sheet->set_owner_node(nullptr);
    sheet->set_owner_css_rule(nullptr);
}

// https://www.w3.org/TR/cssom/#add-a-css-style-sheet
static void add_a_css_style_sheet(DOM::Document& document, NonnullRefPtr<CSS::CSSStyleSheet> sheet)
{
    // 1. Add the CSS style sheet to the list of document or shadow root CSS style sheets at the appropriate location. The remainder of these steps deal with the disabled flag.
    document.style_sheets().add_sheet(sheet);

    // 2. If the disabled flag is set, then return.
    if (sheet->disabled())
        return;

    // FIXME: 3. If the title is not the empty string, the alternate flag is unset, and preferred CSS style sheet set name is the empty string change the preferred CSS style sheet set name to the title.

    // FIXME: 4. If any of the following is true, then unset the disabled flag and return:
    //           The title is the empty string.
    //           The last CSS style sheet set name is null and the title is a case-sensitive match for the preferred CSS style sheet set name.
    //           The title is a case-sensitive match for the last CSS style sheet set name.

    // FIXME: 5. Set the disabled flag.
}

// https://www.w3.org/TR/cssom/#create-a-css-style-sheet
static void create_a_css_style_sheet(DOM::Document& document, String type, DOM::Element* owner_node, String media, String title, bool alternate, bool origin_clean, void* location, CSS::CSSStyleSheet* parent_style_sheet, CSS::CSSRule* owner_rule, NonnullRefPtr<CSS::CSSStyleSheet> sheet)
{
    // 1. Create a new CSS style sheet object and set its properties as specified.
    // FIXME: We receive `sheet` from the caller already. This is weird.

    sheet->set_parent_css_style_sheet(parent_style_sheet);
    sheet->set_owner_css_rule(owner_rule);
    sheet->set_owner_node(owner_node);
    sheet->set_type(move(type));
    sheet->set_media(move(media));
    sheet->set_title(move(title));
    sheet->set_alternate(alternate);
    sheet->set_origin_clean(origin_clean);
    (void)location;

    // 2. Then run the add a CSS style sheet steps for the newly created CSS style sheet.
    add_a_css_style_sheet(document, move(sheet));
}

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
void HTMLStyleElement::update_a_style_block()
{
    // 1. Let element be the style element.
    // 2. If element has an associated CSS style sheet, remove the CSS style sheet in question.

    if (m_associated_css_style_sheet) {
        remove_a_css_style_sheet(document(), *m_associated_css_style_sheet);

        // FIXME: This should probably be handled by StyleSheet::set_owner_node().
        m_associated_css_style_sheet = nullptr;
    }

    // 3. If element is not connected, then return.
    if (!is_connected())
        return;

    // 4. If element's type attribute is present and its value is neither the empty string nor an ASCII case-insensitive match for "text/css", then return.
    auto type_attribute = attribute(HTML::AttributeNames::type);
    if (!type_attribute.is_null() && !type_attribute.is_empty() && !type_attribute.equals_ignoring_case("text/css"sv))
        return;

    // FIXME: 5. If the Should element's inline behavior be blocked by Content Security Policy? algorithm returns "Blocked" when executed upon the style element, "style", and the style element's child text content, then return. [CSP]

    // FIXME: This is a bit awkward, as the spec doesn't actually tell us when to parse the CSS text,
    //        so we just do it here and pass the parsed sheet to create_a_css_style_sheet().
    auto sheet = parse_css(CSS::ParsingContext(document()), text_content());
    if (!sheet)
        return;

    // FIXME: This should probably be handled by StyleSheet::set_owner_node().
    m_associated_css_style_sheet = sheet;

    // 6. Create a CSS style sheet with the following properties...
    create_a_css_style_sheet(
        document(),
        "text/css"sv,
        this,
        attribute(HTML::AttributeNames::media),
        in_a_document_tree() ? attribute(HTML::AttributeNames::title) : String::empty(),
        false,
        true,
        nullptr,
        nullptr,
        nullptr,
        sheet.release_nonnull());
}

// https://www.w3.org/TR/cssom/#dom-linkstyle-sheet
RefPtr<CSS::CSSStyleSheet> HTMLStyleElement::sheet() const
{
    // The sheet attribute must return the associated CSS style sheet for the node or null if there is no associated CSS style sheet.
    return m_associated_css_style_sheet;
}

}
