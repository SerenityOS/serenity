/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/HTMLParamElement.h>

namespace Web::HTML {

HTMLParamElement::HTMLParamElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLParamElement::~HTMLParamElement() = default;

JS::ThrowCompletionOr<void> HTMLParamElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLParamElementPrototype>(realm, "HTMLParamElement"));

    return {};
}

}
