/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLAudioElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLAudioElement::HTMLAudioElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLMediaElement(document, move(qualified_name))
{
    set_prototype(&window().cached_web_prototype("HTMLAudioElement"));
}

HTMLAudioElement::~HTMLAudioElement() = default;
}
