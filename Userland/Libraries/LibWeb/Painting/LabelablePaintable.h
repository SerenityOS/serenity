/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/LabelableNode.h>
#include <LibWeb/Painting/Paintable.h>

namespace Web::Painting {

class LabelablePaintable : public PaintableBox {
public:
    Layout::LabelableNode const& layout_box() const;
    Layout::LabelableNode& layout_box();

    virtual void handle_associated_label_mousedown(Badge<Layout::Label>) { }
    virtual void handle_associated_label_mouseup(Badge<Layout::Label>) { }
    virtual void handle_associated_label_mousemove(Badge<Layout::Label>, [[maybe_unused]] bool is_inside_node_or_label) { }

protected:
    LabelablePaintable(Layout::LabelableNode const&);
};

}
