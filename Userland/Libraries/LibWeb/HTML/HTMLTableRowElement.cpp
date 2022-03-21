/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/HTML/HTMLTableCellElement.h>
#include <LibWeb/HTML/HTMLTableElement.h>
#include <LibWeb/HTML/HTMLTableRowElement.h>
#include <LibWeb/HTML/HTMLTableSectionElement.h>

namespace Web::HTML {

HTMLTableRowElement::HTMLTableRowElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
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

void HTMLTableRowElement::inserted()
{
    auto row_index_in_collection = [&](auto const& collection) -> long {
        auto elements = collection->collect_matching_elements();

        for (size_t i = 0; i < elements.size(); ++i) {
            if (elements[i] == this)
                return i;
        }

        return -1;
    };

    // https://html.spec.whatwg.org/multipage/tables.html#dom-tr-rowindex
    auto determine_row_index = [&]() -> long {
        auto* table = first_ancestor_of_type<HTMLTableElement>();
        return table ? row_index_in_collection(table->rows()) : -1;
    };

    // https://html.spec.whatwg.org/multipage/tables.html#dom-tr-sectionrowindex
    auto determine_section_row_index = [&]() -> long {
        for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
            if (is<HTMLTableElement>(*ancestor))
                return row_index_in_collection(static_cast<HTMLTableElement&>(*ancestor).rows());
            if (is<HTMLTableSectionElement>(*ancestor))
                return row_index_in_collection(static_cast<HTMLTableSectionElement&>(*ancestor).rows());
        }

        return -1;
    };

    m_row_index = determine_row_index();
    m_section_row_index = determine_section_row_index();
}

void HTMLTableRowElement::removed_from(Node*)
{
    m_row_index = -1;
    m_section_row_index = -1;
}

}
