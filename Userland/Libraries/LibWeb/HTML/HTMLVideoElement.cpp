/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLVideoElement.h>

namespace Web::HTML {

HTMLVideoElement::HTMLVideoElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLMediaElement(document, move(qualified_name))
{
}

HTMLVideoElement::~HTMLVideoElement() = default;

void HTMLVideoElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLVideoElementPrototype>(realm, "HTMLVideoElement"));
}

}
