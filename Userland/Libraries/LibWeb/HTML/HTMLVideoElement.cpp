/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLVideoElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLVideoElement::HTMLVideoElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLMediaElement(document, move(qualified_name))
{
    set_prototype(&window().cached_web_prototype("HTMLVideoElement"));
}

HTMLVideoElement::~HTMLVideoElement() = default;

}
