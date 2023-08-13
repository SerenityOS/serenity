/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLOptGroupElement.h>
#include <LibWeb/HTML/HTMLOptionElement.h>
#include <LibWeb/HTML/HTMLOptionsCollection.h>
#include <LibWeb/HTML/HTMLSelectElement.h>
#include <LibWeb/WebIDL/DOMException.h>

namespace Web::HTML {

JS::NonnullGCPtr<HTMLOptionsCollection> HTMLOptionsCollection::create(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter)
{
    return root.heap().allocate<HTMLOptionsCollection>(root.realm(), root, move(filter));
}

HTMLOptionsCollection::HTMLOptionsCollection(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter)
    : DOM::HTMLCollection(root, Scope::Descendants, move(filter))
{
}

HTMLOptionsCollection::~HTMLOptionsCollection() = default;

void HTMLOptionsCollection::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLOptionsCollectionPrototype>(realm, "HTMLOptionsCollection"));
}

// https://html.spec.whatwg.org/multipage/common-dom-interfaces.html#dom-htmloptionscollection-add
WebIDL::ExceptionOr<void> HTMLOptionsCollection::add(HTMLOptionOrOptGroupElement element, Optional<HTMLElementOrElementIndex> before)
{
    auto resolved_element = element.visit(
        [](auto& e) -> JS::Handle<HTMLElement> {
            return JS::make_handle(static_cast<HTML::HTMLElement&>(*e));
        });

    JS::GCPtr<DOM::Node> before_element;
    if (before.has_value() && before->has<JS::Handle<HTMLElement>>())
        before_element = before->get<JS::Handle<HTMLElement>>().ptr();

    // 1. If element is an ancestor of the select element on which the HTMLOptionsCollection is rooted, then throw a "HierarchyRequestError" DOMException.
    if (resolved_element->is_ancestor_of(root()))
        return WebIDL::HierarchyRequestError::create(realm(), "The provided element is an ancestor of the root select element.");

    // 2. If before is an element, but that element isn't a descendant of the select element on which the HTMLOptionsCollection is rooted, then throw a "NotFoundError" DOMException.
    if (before_element && !before_element->is_descendant_of(root()))
        return WebIDL::NotFoundError::create(realm(), "The 'before' element is not a descendant of the root select element.");

    // 3. If element and before are the same element, then return.
    if (before_element && (resolved_element.ptr() == before_element.ptr()))
        return {};

    // 4. If before is a node, then let reference be that node. Otherwise, if before is an integer, and there is a beforeth node in the collection, let reference be that node. Otherwise, let reference be null.
    JS::GCPtr<DOM::Node> reference;

    if (before_element)
        reference = move(before_element);
    else if (before.has_value() && before->has<i32>())
        reference = item(before->get<i32>());

    // 5. If reference is not null, let parent be the parent node of reference. Otherwise, let parent be the select element on which the HTMLOptionsCollection is rooted.
    DOM::Node* parent = reference ? reference->parent() : root().ptr();

    // 6. Pre-insert element into parent node before reference.
    (void)TRY(parent->pre_insert(*resolved_element, reference));

    return {};
}

}
