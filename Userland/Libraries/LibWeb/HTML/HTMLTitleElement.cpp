/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLTitleElement.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

HTMLTitleElement::HTMLTitleElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTitleElement::~HTMLTitleElement()
{
}

void HTMLTitleElement::children_changed()
{
    HTMLElement::children_changed();
    if (auto* page = document().page()) {
        if (document().browsing_context() == &page->top_level_browsing_context())
            page->client().page_did_change_title(document().title());
    }
}

}
