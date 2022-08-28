/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLTableRowElementPrototype.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/HTML/HTMLTableCellElement.h>
#include <LibWeb/HTML/HTMLTableElement.h>
#include <LibWeb/HTML/HTMLTableRowElement.h>
#include <LibWeb/HTML/HTMLTableSectionElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLTableRowElement::HTMLTableRowElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLTableRowElementPrototype>("HTMLTableRowElement"));
}

HTMLTableRowElement::~HTMLTableRowElement() = default;

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

// https://html.spec.whatwg.org/multipage/tables.html#dom-tr-rowindex
int HTMLTableRowElement::row_index() const
{
    // The rowIndex attribute must, if this element has a parent table element,
    // or a parent tbody, thead, or tfoot element and a grandparent table element,
    // return the index of this tr element in that table element's rows collection.
    // If there is no such table element, then the attribute must return −1.
    auto rows_collection = [&]() -> RefPtr<DOM::HTMLCollection> {
        if (!parent())
            return nullptr;
        if (is<HTMLTableElement>(*parent()))
            return const_cast<HTMLTableElement&>(static_cast<HTMLTableElement const&>(*parent())).rows();
        if (is<HTMLTableSectionElement>(*parent()) && parent()->parent() && is<HTMLTableElement>(*parent()->parent()))
            return const_cast<HTMLTableElement&>(static_cast<HTMLTableElement const&>(*parent()->parent())).rows();
        return nullptr;
    }();
    if (!rows_collection)
        return -1;
    auto rows = rows_collection->collect_matching_elements();
    for (size_t i = 0; i < rows.size(); ++i) {
        if (rows[i] == this)
            return i;
    }
    return -1;
}

int HTMLTableRowElement::section_row_index() const
{
    // The sectionRowIndex attribute must, if this element has a parent table, tbody, thead, or tfoot element,
    // return the index of the tr element in the parent element's rows collection
    // (for tables, that's HTMLTableElement's rows collection; for table sections, that's HTMLTableSectionElement's rows collection).
    // If there is no such parent element, then the attribute must return −1.
    auto rows_collection = [&]() -> RefPtr<DOM::HTMLCollection> {
        if (!parent())
            return nullptr;
        if (is<HTMLTableElement>(*parent()))
            return const_cast<HTMLTableElement&>(static_cast<HTMLTableElement const&>(*parent())).rows();
        if (is<HTMLTableSectionElement>(*parent()))
            return static_cast<HTMLTableSectionElement const&>(*parent()).rows();
        return nullptr;
    }();
    if (!rows_collection)
        return -1;
    auto rows = rows_collection->collect_matching_elements();
    for (size_t i = 0; i < rows.size(); ++i) {
        if (rows[i] == this)
            return i;
    }
    return -1;
}

}
