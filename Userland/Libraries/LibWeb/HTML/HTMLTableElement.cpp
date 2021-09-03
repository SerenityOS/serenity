/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Adam Hodgen <ant1441@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/HTML/HTMLTableColElement.h>
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

RefPtr<HTMLTableCaptionElement> HTMLTableElement::caption()
{
    return first_child_of_type<HTMLTableCaptionElement>();
}

void HTMLTableElement::set_caption(HTMLTableCaptionElement* caption)
{
    // FIXME: This is not always the case, but this function is currently written in a way that assumes non-null.
    VERIFY(caption);

    // FIXME: The spec requires deleting the current caption if caption is null
    //        Currently the wrapper generator doesn't send us a nullable value
    delete_caption();

    pre_insert(*caption, first_child());
}

NonnullRefPtr<HTMLTableCaptionElement> HTMLTableElement::create_caption()
{
    auto maybe_caption = caption();
    if (maybe_caption) {
        return *maybe_caption;
    }

    auto caption = DOM::create_element(document(), TagNames::caption, Namespace::HTML);
    pre_insert(caption, first_child());
    return static_ptr_cast<HTMLTableCaptionElement>(caption);
}

void HTMLTableElement::delete_caption()
{
    auto maybe_caption = caption();
    if (maybe_caption) {
        maybe_caption->remove(false);
    }
}

RefPtr<HTMLTableSectionElement> HTMLTableElement::t_head()
{
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        if (is<HTMLTableSectionElement>(*child)) {
            auto table_section_element = &verify_cast<HTMLTableSectionElement>(*child);
            if (table_section_element->local_name() == TagNames::thead)
                return table_section_element;
        }
    }

    return nullptr;
}

DOM::ExceptionOr<void> HTMLTableElement::set_t_head(HTMLTableSectionElement* thead)
{
    // FIXME: This is not always the case, but this function is currently written in a way that assumes non-null.
    VERIFY(thead);

    if (thead->local_name() != TagNames::thead)
        return DOM::HierarchyRequestError::create("Element is not thead");

    // FIXME: The spec requires deleting the current thead if thead is null
    //        Currently the wrapper generator doesn't send us a nullable value
    delete_t_head();

    // We insert the new thead after any <caption> or <colgroup> elements
    DOM::Node* child_to_append_after = nullptr;
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        if (!is<HTMLElement>(*child))
            continue;
        if (is<HTMLTableCaptionElement>(*child))
            continue;
        if (is<HTMLTableColElement>(*child)) {
            auto table_col_element = &verify_cast<HTMLTableColElement>(*child);
            if (table_col_element->local_name() == TagNames::colgroup)
                continue;
        }

        // We have found an element which is not a <caption> or <colgroup>, we'll insert before this
        child_to_append_after = child;
        break;
    }

    pre_insert(*thead, child_to_append_after);

    return {};
}

NonnullRefPtr<HTMLTableSectionElement> HTMLTableElement::create_t_head()
{
    auto maybe_thead = t_head();
    if (maybe_thead)
        return *maybe_thead;

    auto thead = DOM::create_element(document(), TagNames::thead, Namespace::HTML);

    // We insert the new thead after any <caption> or <colgroup> elements
    DOM::Node* child_to_append_after = nullptr;
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        if (!is<HTMLElement>(*child))
            continue;
        if (is<HTMLTableCaptionElement>(*child))
            continue;
        if (is<HTMLTableColElement>(*child)) {
            auto table_col_element = &verify_cast<HTMLTableColElement>(*child);
            if (table_col_element->local_name() == TagNames::colgroup)
                continue;
        }

        // We have found an element which is not a <caption> or <colgroup>, we'll insert before this
        child_to_append_after = child;
        break;
    }

    pre_insert(thead, child_to_append_after);

    return static_ptr_cast<HTMLTableSectionElement>(thead);
}

void HTMLTableElement::delete_t_head()
{
    auto maybe_thead = t_head();
    if (maybe_thead) {
        maybe_thead->remove(false);
    }
}

RefPtr<HTMLTableSectionElement> HTMLTableElement::t_foot()
{
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        if (is<HTMLTableSectionElement>(*child)) {
            auto table_section_element = &verify_cast<HTMLTableSectionElement>(*child);
            if (table_section_element->local_name() == TagNames::tfoot)
                return table_section_element;
        }
    }

    return nullptr;
}

DOM::ExceptionOr<void> HTMLTableElement::set_t_foot(HTMLTableSectionElement* tfoot)
{
    // FIXME: This is not always the case, but this function is currently written in a way that assumes non-null.
    VERIFY(tfoot);

    if (tfoot->local_name() != TagNames::tfoot)
        return DOM::HierarchyRequestError::create("Element is not tfoot");

    // FIXME: The spec requires deleting the current tfoot if tfoot is null
    //        Currently the wrapper generator doesn't send us a nullable value
    delete_t_foot();

    // We insert the new tfoot at the end of the table
    append_child(*tfoot);

    return {};
}

NonnullRefPtr<HTMLTableSectionElement> HTMLTableElement::create_t_foot()
{
    auto maybe_tfoot = t_foot();
    if (maybe_tfoot)
        return *maybe_tfoot;

    auto tfoot = DOM::create_element(document(), TagNames::tfoot, Namespace::HTML);
    append_child(tfoot);
    return static_ptr_cast<HTMLTableSectionElement>(tfoot);
}

void HTMLTableElement::delete_t_foot()
{
    auto maybe_tfoot = t_foot();
    if (maybe_tfoot) {
        maybe_tfoot->remove(false);
    }
}

NonnullRefPtr<DOM::HTMLCollection> HTMLTableElement::t_bodies()
{
    return DOM::HTMLCollection::create(*this, [](DOM::Element const& element) {
        return element.local_name() == TagNames::tbody;
    });
}

NonnullRefPtr<HTMLTableSectionElement> HTMLTableElement::create_t_body()
{
    auto tbody = DOM::create_element(document(), TagNames::tbody, Namespace::HTML);

    // We insert the new tbody after the last <tbody> element
    DOM::Node* child_to_append_after = nullptr;
    for (auto* child = last_child(); child; child = child->previous_sibling()) {
        if (!is<HTMLElement>(*child))
            continue;
        if (is<HTMLTableSectionElement>(*child)) {
            auto table_section_element = &verify_cast<HTMLTableSectionElement>(*child);
            if (table_section_element->local_name() == TagNames::tbody) {
                // We have found an element which is a <tbody> we'll insert after this
                child_to_append_after = child->next_sibling();
                break;
            }
        }
    }

    pre_insert(tbody, child_to_append_after);

    return static_ptr_cast<HTMLTableSectionElement>(tbody);
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

        if (element.parent_element() && (element.parent_element()->local_name() == TagNames::thead || element.parent_element()->local_name() == TagNames::tbody || element.parent_element()->local_name() == TagNames::tfoot)
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
    auto tr = static_ptr_cast<HTMLTableRowElement>(DOM::create_element(document(), TagNames::tr, Namespace::HTML));
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
