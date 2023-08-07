/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLTrackElement.h>

namespace Web::HTML {

HTMLTrackElement::HTMLTrackElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLTrackElement::~HTMLTrackElement() = default;

void HTMLTrackElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLTrackElementPrototype>(realm, "HTMLTrackElement"));
}

}
