/*
 * Copyright (c) 2022, Johan Dahlin <jdahlin@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLFormControlsCollection.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>

namespace Web::HTML {

JS::NonnullGCPtr<HTMLFormControlsCollection> HTMLFormControlsCollection::create(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter)
{
    return *root.heap().allocate<HTMLFormControlsCollection>(root.realm(), root, move(filter));
}

HTMLFormControlsCollection::HTMLFormControlsCollection(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter)
    : DOM::HTMLCollection(root, move(filter))
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLFormControlsCollection"));
}

HTMLFormControlsCollection::~HTMLFormControlsCollection() = default;

// https://html.spec.whatwg.org/multipage/common-dom-interfaces.html#dom-htmlformcontrolscollection-nameditem
Variant<JS::Handle<RadioNodeList>, JS::Handle<DOM::Element>, Empty> HTMLFormControlsCollection::named_item(FlyString const& name)
{
    // 1. If name is the empty string, return null and stop the algorithm.
    if (name.is_empty())
        return {};

    // 2. If, at the time the method is called, there is exactly one node in the collection
    // that has either an id attribute or a name attribute equal to name, then return that node and stop the algorithm.
    auto elements = root()->children();
    if (elements->length() == 1) {
        auto child = elements->item(0);
        if (is<FormAssociatedElement>(child)) {
            auto form_associated_element = dynamic_cast<HTML::FormAssociatedElement*>(child);
            if (!form_associated_element->is_listed()) {
                auto& html_element = form_associated_element->form_associated_element_to_html_element();
                if (html_element.get_attribute(AttributeNames::id) == name || html_element.get_attribute(AttributeNames::name) == name)
                    return JS::make_handle(child);
            }
        }
    }

    // 3. Otherwise, if there are no nodes in the collection that have either an id attribute or a name
    // attribute equal to name, then return null and stop the algorithm.
    auto found = false;
    for (size_t i = 0; i < elements->length(); ++i) {
        auto child = elements->item(i);
        if (!is<FormAssociatedElement>(child))
            continue;
        auto form_associated_element = dynamic_cast<HTML::FormAssociatedElement*>(child);
        if (!form_associated_element->is_listed())
            continue;
        auto& html_element = form_associated_element->form_associated_element_to_html_element();
        if (html_element.get_attribute(AttributeNames::id) == name || html_element.get_attribute(AttributeNames::name) == name) {
            found = true;
            break;
        }
    }
    if (!found) {
        return {};
    }

    // 4. Otherwise, create a new RadioNodeList object representing a live view of the HTMLFormControlsCollection object,
    // further filtered so that the only nodes in the RadioNodeList object are those that have either an id attribute or
    // a name attribute equal to name. The nodes in the RadioNodeList object must be sorted in tree order.
    TODO();

    // 5. Return that RadioNodeList object.
    return {};
}

}
