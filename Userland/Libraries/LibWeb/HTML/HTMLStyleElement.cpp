/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/StringBuilder.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLStyleElement.h>

namespace Web::HTML {

HTMLStyleElement::HTMLStyleElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
    , m_css_loader(*this)
{
    m_css_loader.on_load = [&] {
        document.update_style();
    };
}

HTMLStyleElement::~HTMLStyleElement()
{
}

void HTMLStyleElement::children_changed()
{
    StringBuilder builder;
    for_each_child([&](auto& child) {
        if (is<DOM::Text>(child))
            builder.append(verify_cast<DOM::Text>(child).text_content());
    });
    m_css_loader.load_from_text(builder.to_string());

    if (auto sheet = m_css_loader.style_sheet())
        document().style_sheets().add_sheet(sheet.release_nonnull());

    HTMLElement::children_changed();
}

void HTMLStyleElement::removed_from(Node* old_parent)
{
    if (m_css_loader.style_sheet()) {
        // FIXME: Remove the sheet from the document
    }
    return HTMLElement::removed_from(old_parent);
}

}
