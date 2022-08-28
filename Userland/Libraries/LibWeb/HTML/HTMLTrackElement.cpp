/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLTrackElementPrototype.h>
#include <LibWeb/HTML/HTMLTrackElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLTrackElement::HTMLTrackElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLTrackElementPrototype>("HTMLTrackElement"));
}

HTMLTrackElement::~HTMLTrackElement() = default;

}
