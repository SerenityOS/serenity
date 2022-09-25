/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLDirectoryElement.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

HTMLDirectoryElement::HTMLDirectoryElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "HTMLDirectoryElement"));
}

HTMLDirectoryElement::~HTMLDirectoryElement() = default;
}
