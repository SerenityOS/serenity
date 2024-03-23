/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLLabelElement.h>
#include <LibWeb/Layout/Label.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLLabelElement);

HTMLLabelElement::HTMLLabelElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLLabelElement::~HTMLLabelElement() = default;

void HTMLLabelElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(HTMLLabelElement);
}

JS::GCPtr<Layout::Node> HTMLLabelElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::Label>(document(), this, move(style));
}

// https://html.spec.whatwg.org/multipage/forms.html#labeled-control
JS::GCPtr<HTMLElement> HTMLLabelElement::control() const
{
    JS::GCPtr<HTMLElement> control;

    // The for attribute may be specified to indicate a form control with which the caption is
    // to be associated. If the attribute is specified, the attribute's value must be the ID of
    // a labelable element in the same tree as the label element. If the attribute is specified
    // and there is an element in the tree whose ID is equal to the value of the for attribute,
    // and the first such element in tree order is a labelable element, then that element is the
    // label element's labeled control.
    if (for_().has_value()) {
        for_each_in_inclusive_subtree_of_type<HTMLElement>([&](auto& element) {
            if (element.id() == *for_() && element.is_labelable()) {
                control = &const_cast<HTMLElement&>(element);
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        return control;
    }

    // If the for attribute is not specified, but the label element has a labelable element descendant,
    // then the first such descendant in tree order is the label element's labeled control.
    for_each_in_subtree_of_type<HTMLElement>([&](auto& element) {
        if (element.is_labelable()) {
            control = &const_cast<HTMLElement&>(element);
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    return control;
}

}
