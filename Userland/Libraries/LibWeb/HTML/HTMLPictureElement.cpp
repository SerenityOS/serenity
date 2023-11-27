/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLPictureElement.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLPictureElement);

HTMLPictureElement::HTMLPictureElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLPictureElement::~HTMLPictureElement() = default;

void HTMLPictureElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLPictureElementPrototype>(realm, "HTMLPictureElement"_fly_string));
}

}
