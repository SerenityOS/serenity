/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLMapElement.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLMapElement);

HTMLMapElement::HTMLMapElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLMapElement::~HTMLMapElement() = default;

void HTMLMapElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(HTMLMapElement);
}

}
