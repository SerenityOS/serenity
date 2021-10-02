/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/HTML/HTMLTemplateElement.h>

namespace Web::DOMParsing {

// https://w3c.github.io/DOM-Parsing/#dom-innerhtml-innerhtml
DOM::ExceptionOr<void> inner_html_setter(NonnullRefPtr<DOM::Node> context_object, String const& value);

}
