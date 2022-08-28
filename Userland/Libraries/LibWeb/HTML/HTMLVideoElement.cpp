/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLVideoElementPrototype.h>
#include <LibWeb/HTML/HTMLVideoElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLVideoElement::HTMLVideoElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLMediaElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLVideoElementPrototype>("HTMLVideoElement"));
}

HTMLVideoElement::~HTMLVideoElement() = default;

}
