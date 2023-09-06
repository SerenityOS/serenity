/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
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

void HTMLTableSectionElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLTableSectionElementPrototype>(realm, "HTMLTableSectionElement"));
}

void HTMLTableSectionElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_rows);
}

// https://html.spec.whatwg.org/multipage/tables.html#dom-tbody-rows
JS::NonnullGCPtr<DOM::HTMLCollection> HTMLTableSectionElement::rows() const
{
    // The rows attribute must return an HTMLCollection rooted at this element,
    // whose filter matches only tr elements that are children of this element.
    if (!m_rows) {
        m_rows = DOM::HTMLCollection::create(const_cast<HTMLTableSectionElement&>(*this), DOM::HTMLCollection::Scope::Children, [](Element const& element) {
            return is<HTMLTableRowElement>(element);
        });
    }
    return *m_rows;
}

// https://html.spec.whatwg.org/multipage/tables.html#dom-tbody-insertrow
WebIDL::ExceptionOr<JS::NonnullGCPtr<HTMLTableRowElement>> HTMLTableSectionElement::insert_row(long index)
{
    auto rows_collection = rows();
    auto rows_collection_size = static_cast<long>(rows_collection->length());

    // 1. If index is less than −1 or greater than the number of elements in the rows collection, throw an "IndexSizeError" DOMException.
    if (index < -1 || index > rows_collection_size)
        return WebIDL::IndexSizeError::create(realm(), "Index is negative or greater than the number of rows"_fly_string);

    // 2. Let table row be the result of creating an element given this element's node document, tr, and the HTML namespace.
    auto& table_row = static_cast<HTMLTableRowElement&>(*TRY(DOM::create_element(document(), TagNames::tr, Namespace::HTML)));

    // 3. If index is −1 or equal to the number of items in the rows collection, then append table row to this element.
    if (index == -1 || index == rows_collection_size)
        TRY(append_child(table_row));
    // 4. Otherwise, insert table row as a child of this element, immediately before the index-th tr element in the rows collection.
    else
        table_row.insert_before(*this, rows_collection->item(index));

    // 5. Return table row.
    return JS::NonnullGCPtr(table_row);
}

// https://html.spec.whatwg.org/multipage/tables.html#dom-tbody-deleterow
WebIDL::ExceptionOr<void> HTMLTableSectionElement::delete_row(long index)
{
    auto rows_collection = rows();
    auto rows_collection_size = static_cast<long>(rows_collection->length());

    // 1. If index is less than −1 or greater than or equal to the number of elements in the rows collection, then throw an "IndexSizeError" DOMException.
    if (index < -1 || index >= rows_collection_size)
        return WebIDL::IndexSizeError::create(realm(), "Index is negative or greater than or equal to the number of rows"_fly_string);

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
