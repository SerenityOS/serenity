/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLMetaElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLMetaElement::HTMLMetaElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().cached_web_prototype("HTMLMetaElement"));
}

HTMLMetaElement::~HTMLMetaElement() = default;

}
