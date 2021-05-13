/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLLegendElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLLegendElementWrapper;

    HTMLLegendElement(DOM::Document&, QualifiedName);
    virtual ~HTMLLegendElement() override;

    virtual RefPtr<Layout::Node> create_layout_node() override;
};

}
