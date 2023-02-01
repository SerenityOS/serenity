/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLTimeElement.h>

namespace Web::HTML {

HTMLTimeElement::HTMLTimeElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

JS::ThrowCompletionOr<void> HTMLTimeElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLTimeElementPrototype>(realm, "HTMLTimeElement"));

    return {};
}

HTMLTimeElement::~HTMLTimeElement() = default;

}
