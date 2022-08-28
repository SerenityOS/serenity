/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLAudioElementPrototype.h>
#include <LibWeb/HTML/HTMLAudioElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLAudioElement::HTMLAudioElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLMediaElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLAudioElementPrototype>("HTMLAudioElement"));
}

HTMLAudioElement::~HTMLAudioElement() = default;
}
