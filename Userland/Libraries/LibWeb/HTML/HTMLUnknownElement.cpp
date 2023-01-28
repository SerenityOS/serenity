/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLUnknownElement.h>

namespace Web::HTML {

HTMLUnknownElement::HTMLUnknownElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLUnknownElement::~HTMLUnknownElement() = default;

JS::ThrowCompletionOr<void> HTMLUnknownElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLUnknownElementPrototype>(realm, "HTMLUnknownElement"));

    return {};
}

}
