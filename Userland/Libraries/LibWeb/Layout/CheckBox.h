/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/Layout/LabelableNode.h>

namespace Web::Layout {

class CheckBox : public LabelableNode {
public:
    CheckBox(DOM::Document&, HTML::HTMLInputElement&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~CheckBox() override;

    const HTML::HTMLInputElement& dom_node() const { return static_cast<const HTML::HTMLInputElement&>(LabelableNode::dom_node()); }
    HTML::HTMLInputElement& dom_node() { return static_cast<HTML::HTMLInputElement&>(LabelableNode::dom_node()); }

private:
    virtual RefPtr<Painting::Paintable> create_paintable() const override;
};

}
