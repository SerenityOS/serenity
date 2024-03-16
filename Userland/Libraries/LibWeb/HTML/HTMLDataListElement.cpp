/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLDataListElement.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLDataListElement);

HTMLDataListElement::HTMLDataListElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLDataListElement::~HTMLDataListElement() = default;

void HTMLDataListElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(HTMLDataListElement);
}

}
