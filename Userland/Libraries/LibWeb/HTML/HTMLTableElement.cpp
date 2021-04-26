/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/DeprecatedCSSParser.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/HTML/HTMLTableElement.h>
#include <LibWeb/HTML/HTMLTableRowElement.h>
#include <LibWeb/Namespace.h>

namespace Web::HTML {

HTMLTableElement::HTMLTableElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTableElement::~HTMLTableElement()
{
}

void HTMLTableElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name == HTML::AttributeNames::width) {
            if (auto parsed_value = parse_html_length(document(), value))
                style.set_property(CSS::PropertyID::Width, parsed_value.release_nonnull());
            return;
        }
        if (name == HTML::AttributeNames::height) {
            if (auto parsed_value = parse_html_length(document(), value))
                style.set_property(CSS::PropertyID::Height, parsed_value.release_nonnull());
            return;
        }
        if (name == HTML::AttributeNames::bgcolor) {
            auto color = Color::from_string(value);
            if (color.has_value())
                style.set_property(CSS::PropertyID::BackgroundColor, CSS::ColorStyleValue::create(color.value()));
            return;
        }
    });
}

NonnullRefPtr<DOM::HTMLCollection> HTMLTableElement::rows()
{
    HTMLTableElement* table_node = this;
    // FIXME:  The elements in the collection must be ordered such that those elements whose parent is a thead are
    //         included first, in tree order, followed by those elements whose parent is either a table or tbody
    //         element, again in tree order, followed finally by those elements whose parent is a tfoot element,
    //         still in tree order.
    // How do you sort HTMLCollection?

    return DOM::HTMLCollection::create(*this, [table_node](DOM::Element const& element) {
        // Only match TR elements which are:
        // * children of the table element
        // * children of the thead, tbody, or tfoot elements that are themselves children of the table element
        if (!is<HTMLTableRowElement>(element)) {
            return false;
        }
        if (element.parent_element() == table_node)
            return true;

        if (element.parent_element() && (element.parent_element()->tag_name() == TagNames::thead || element.parent_element()->tag_name() == TagNames::tbody || element.parent_element()->tag_name() == TagNames::tfoot)
            && element.parent()->parent() == table_node) {
            return true;
        }

        return false;
    });
}

DOM::ExceptionOr<NonnullRefPtr<HTMLTableRowElement>> HTMLTableElement::insert_row(long index)
{
    auto rows = this->rows();
    auto rows_length = rows->length();

    if (index < -1 || index >= (long)rows_length) {
        return DOM::IndexSizeError::create("Index is negative or greater than the number of rows");
    }
    auto tr = static_cast<NonnullRefPtr<HTMLTableRowElement>>(DOM::create_element(document(), TagNames::tr, Namespace::HTML));
    if (rows_length == 0 && !has_child_of_type<HTMLTableRowElement>()) {
        auto tbody = DOM::create_element(document(), TagNames::tbody, Namespace::HTML);
        tbody->append_child(tr);
        append_child(tbody);
    } else if (rows_length == 0) {
        auto tbody = last_child_of_type<HTMLTableRowElement>();
        tbody->append_child(tr);
    } else if (index == -1 || index == (long)rows_length) {
        auto parent_of_last_tr = rows->item(rows_length - 1)->parent_element();
        parent_of_last_tr->append_child(tr);
    } else {
        rows->item(index)->parent_element()->insert_before(tr, rows->item(index));
    }
    return tr;
}

DOM::ExceptionOr<void> HTMLTableElement::delete_row(long index)
{
    auto rows = this->rows();
    auto rows_length = rows->length();

    if (index < -1 || index >= (long)rows_length) {
        return DOM::IndexSizeError::create("Index is negative or greater than the number of rows");
    }
    if (index == -1 && rows_length > 0) {
        auto row_to_remove = rows->item(rows_length - 1);
        row_to_remove->remove(false);
    } else {
        auto row_to_remove = rows->item(index);
        row_to_remove->remove(false);
    }

    return {};
}

}
