/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/HTML/HTMLTableRowElement.h>
#include <LibWeb/HTML/HTMLTableSectionElement.h>
#include <LibWeb/Namespace.h>

namespace Web::HTML {

HTMLTableSectionElement::HTMLTableSectionElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTableSectionElement::~HTMLTableSectionElement() = default;

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

// https://html.spec.whatwg.org/multipage/tables.html#dom-tbody-insertrow
DOM::ExceptionOr<NonnullRefPtr<HTMLTableRowElement>> HTMLTableSectionElement::insert_row(long index)
{
    auto rows_collection = rows();
    auto rows_collection_size = static_cast<long>(rows_collection->length());

    // 1. If index is less than −1 or greater than the number of elements in the rows collection, throw an "IndexSizeError" DOMException.
    if (index < -1 || index > rows_collection_size)
        return DOM::IndexSizeError::create("Index is negative or greater than the number of rows");

    // 2. Let table row be the result of creating an element given this element's node document, tr, and the HTML namespace.
    auto table_row = static_ptr_cast<HTMLTableRowElement>(DOM::create_element(document(), TagNames::tr, Namespace::HTML));

    // 3. If index is −1 or equal to the number of items in the rows collection, then append table row to this element.
    if (index == -1 || index == rows_collection_size)
        append_child(table_row);
    // 4. Otherwise, insert table row as a child of this element, immediately before the index-th tr element in the rows collection.
    else
        table_row->insert_before(*this, rows_collection->item(index));

    // 5. Return table row.
    return table_row;
}

// https://html.spec.whatwg.org/multipage/tables.html#dom-tbody-deleterow
DOM::ExceptionOr<void> HTMLTableSectionElement::delete_row(long index)
{
    auto rows_collection = rows();
    auto rows_collection_size = static_cast<long>(rows_collection->length());

    // 1. If index is less than −1 or greater than or equal to the number of elements in the rows collection, then throw an "IndexSizeError" DOMException.
    if (index < -1 || index >= rows_collection_size)
        return DOM::IndexSizeError::create("Index is negative or greater than or equal to the number of rows");

    // 2. If index is −1, then remove the last element in the rows collection from this element, or do nothing if the rows collection is empty.
    if (index == -1) {
        if (rows_collection_size > 0)
            rows_collection->item(rows_collection_size - 1)->remove();
    }
    // 3. Otherwise, remove the indexth element in the rows collection from this element.
    else {
        rows_collection->item(index)->remove();
    }
    return {};
}

}
