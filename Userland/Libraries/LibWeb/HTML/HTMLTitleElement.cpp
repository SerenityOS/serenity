/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLTitleElement.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLTitleElement);

HTMLTitleElement::HTMLTitleElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTitleElement::~HTMLTitleElement() = default;

void HTMLTitleElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLTitleElementPrototype>(realm, "HTMLTitleElement"_fly_string));
}

void HTMLTitleElement::children_changed()
{
    HTMLElement::children_changed();
    if (navigable() && navigable()->is_traversable()) {
        navigable()->traversable_navigable()->page().client().page_did_change_title(document().title().to_byte_string());
    }
}

// https://html.spec.whatwg.org/multipage/semantics.html#dom-title-text
ByteString HTMLTitleElement::text()
{
    // The text attribute's getter must return this title element's child text content.
    return child_text_content().to_byte_string();
}

// https://html.spec.whatwg.org/multipage/semantics.html#dom-title-text
void HTMLTitleElement::set_text(String const& value)
{
    // The text attribute's setter must string replace all with the given value within this title element.
    string_replace_all(value);
}

}
