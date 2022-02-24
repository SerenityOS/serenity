/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLLabelElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLLabelElementWrapper;

    HTMLLabelElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLLabelElement() override;

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    String for_() const { return attribute(HTML::AttributeNames::for_); }
};

}
