/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/Layout/LabelableNode.h>

namespace Web::Layout {

class RadioButton final : public LabelableNode {
public:
    RadioButton(DOM::Document&, HTML::HTMLInputElement&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~RadioButton() override;

    const HTML::HTMLInputElement& dom_node() const { return static_cast<const HTML::HTMLInputElement&>(LabelableNode::dom_node()); }
    HTML::HTMLInputElement& dom_node() { return static_cast<HTML::HTMLInputElement&>(LabelableNode::dom_node()); }

private:
    virtual RefPtr<Painting::Paintable> create_paintable() const override;
};

}
