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
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLVideoElement"));
}

HTMLVideoElement::~HTMLVideoElement() = default;

}
