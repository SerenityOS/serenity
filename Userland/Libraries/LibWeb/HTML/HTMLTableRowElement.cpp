/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/HTML/HTMLTableCellElement.h>
#include <LibWeb/HTML/HTMLTableRowElement.h>

namespace Web::HTML {

HTMLTableRowElement::HTMLTableRowElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTableRowElement::~HTMLTableRowElement()
{
}

// https://html.spec.whatwg.org/multipage/tables.html#dom-tr-cells
NonnullRefPtr<DOM::HTMLCollection> HTMLTableRowElement::cells() const
{
    // The cells attribute must return an HTMLCollection rooted at this tr element,
    // whose filter matches only td and th elements that are children of the tr element.
    // FIXME: This should return the same HTMLCollection object every time,
    //        but that would cause a reference cycle since HTMLCollection refs the root.
    return DOM::HTMLCollection::create(const_cast<HTMLTableRowElement&>(*this), [this](Element const& element) {
        return element.parent() == this
            && is<HTMLTableCellElement>(element);
    });
}

}
