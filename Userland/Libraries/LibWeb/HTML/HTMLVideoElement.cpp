/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLVideoElement.h>

namespace Web::HTML {

HTMLVideoElement::HTMLVideoElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLMediaElement(document, move(qualified_name))
{
}

HTMLVideoElement::~HTMLVideoElement()
{
}

}
