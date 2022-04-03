/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/HTML/HTMLOptGroupElement.h>
#include <LibWeb/HTML/HTMLOptionElement.h>
#include <LibWeb/HTML/HTMLOptionsCollection.h>
#include <LibWeb/HTML/HTMLSelectElement.h>

namespace Web::HTML {

HTMLOptionsCollection::HTMLOptionsCollection(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter)
    : DOM::HTMLCollection(root, move(filter))
{
}

// https://html.spec.whatwg.org/multipage/common-dom-interfaces.html#dom-htmloptionscollection-add
DOM::ExceptionOr<void> HTMLOptionsCollection::add(HTMLOptionOrOptGroupElement element, Optional<HTMLElementOrElementIndex> before)
{
    auto resolved_element = element.visit([](auto const& e) -> NonnullRefPtr<HTMLElement> { return e; });

    RefPtr<DOM::Node> before_element;
    if (before.has_value() && before->has<NonnullRefPtr<HTMLElement>>())
        before_element = before->get<NonnullRefPtr<HTMLElement>>();

    // 1. If element is an ancestor of the select element on which the HTMLOptionsCollection is rooted, then throw a "HierarchyRequestError" DOMException.
    if (resolved_element->is_ancestor_of(root()))
        return DOM::HierarchyRequestError::create("The provided element is an ancestor of the root select element.");

    // 2. If before is an element, but that element isn't a descendant of the select element on which the HTMLOptionsCollection is rooted, then throw a "NotFoundError" DOMException.
    if (before_element && !before_element->is_descendant_of(root()))
        return DOM::NotFoundError::create("The 'before' element is not a descendant of the root select element.");

    // 3. If element and before are the same element, then return.
    if (before_element && (resolved_element.ptr() == before_element.ptr()))
        return {};

    // 4. If before is a node, then let reference be that node. Otherwise, if before is an integer, and there is a beforeth node in the collection, let reference be that node. Otherwise, let reference be null.
    RefPtr<DOM::Node> reference;

    if (before_element)
        reference = move(before_element);
    else if (before.has_value() && before->has<i32>())
        reference = item(before->get<i32>());

    // 5. If reference is not null, let parent be the parent node of reference. Otherwise, let parent be the select element on which the HTMLOptionsCollection is rooted.
    DOM::Node* parent = reference ? reference->parent() : root().ptr();

    // 6. Pre-insert element into parent node before reference.
    (void)TRY(parent->pre_insert(resolved_element, reference));

    return {};
}

}
