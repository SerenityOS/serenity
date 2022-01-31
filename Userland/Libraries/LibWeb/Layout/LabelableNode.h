/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Forward.h>
#include <LibWeb/Layout/ReplacedBox.h>

namespace Web::Layout {

class LabelableNode : public ReplacedBox {
public:
    virtual void handle_associated_label_mousedown(Badge<Label>) { }
    virtual void handle_associated_label_mouseup(Badge<Label>) { }
    virtual void handle_associated_label_mousemove(Badge<Label>, [[maybe_unused]] bool is_inside_node_or_label) { }

protected:
    LabelableNode(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
        : ReplacedBox(document, element, move(style))
    {
    }

    virtual ~LabelableNode() = default;
};

}
