/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLHRElement.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLHRElement);

HTMLHRElement::HTMLHRElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLHRElement::~HTMLHRElement() = default;

void HTMLHRElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLHRElementPrototype>(realm, "HTMLHRElement"_fly_string));
}

}
