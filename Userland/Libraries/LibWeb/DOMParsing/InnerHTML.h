/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/HTML/HTMLTemplateElement.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::DOMParsing {

// https://w3c.github.io/DOM-Parsing/#dom-innerhtml-innerhtml
WebIDL::ExceptionOr<void> inner_html_setter(JS::NonnullGCPtr<DOM::Node> context_object, StringView value);

WebIDL::ExceptionOr<JS::NonnullGCPtr<DOM::DocumentFragment>> parse_fragment(StringView markup, DOM::Element& context_element);

}
