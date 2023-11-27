/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLMeterElement.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLMeterElement);

HTMLMeterElement::HTMLMeterElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLMeterElement::~HTMLMeterElement() = default;

void HTMLMeterElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLMeterElementPrototype>(realm, "HTMLMeterElement"_fly_string));
}

}
