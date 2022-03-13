/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLProgressElement.h>
#include <LibWeb/Layout/LabelableNode.h>

namespace Web::Layout {

class Progress : public LabelableNode {
public:
    Progress(DOM::Document&, HTML::HTMLProgressElement&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~Progress() override;

    const HTML::HTMLProgressElement& dom_node() const { return static_cast<const HTML::HTMLProgressElement&>(LabelableNode::dom_node()); }
    HTML::HTMLProgressElement& dom_node() { return static_cast<HTML::HTMLProgressElement&>(LabelableNode::dom_node()); }

    virtual RefPtr<Painting::Paintable> create_paintable() const override;
};

}
