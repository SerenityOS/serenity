/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/HTML/HTMLTableRowElement.h>
#include <LibWeb/HTML/HTMLTableSectionElement.h>

namespace Web::HTML {

HTMLTableSectionElement::HTMLTableSectionElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTableSectionElement::~HTMLTableSectionElement()
{
}

// https://html.spec.whatwg.org/multipage/tables.html#dom-tbody-rows
NonnullRefPtr<DOM::HTMLCollection> HTMLTableSectionElement::rows() const
{
    // The rows attribute must return an HTMLCollection rooted at this element,
    // whose filter matches only tr elements that are children of this element.
    // FIXME: This should return the same HTMLCollection object every time,
    //        but that would cause a reference cycle since HTMLCollection refs the root.
    return DOM::HTMLCollection::create(const_cast<HTMLTableSectionElement&>(*this), [this](Element const& element) {
        return element.parent() == this
            && is<HTMLTableRowElement>(element);
    });
}

}
