/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGUI/Event.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Layout/LabelableNode.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Page/Frame.h>

namespace Web::Layout {

Label::Label(DOM::Document& document, HTML::HTMLLabelElement* element, NonnullRefPtr<CSS::StyleProperties> style)
    : BlockBox(document, element, move(style))
{
}

Label::~Label()
{
}

void Label::handle_mousedown_on_label(Badge<TextNode>, const Gfx::IntPoint&, unsigned button)
{
    if (button != GUI::MouseButton::Left)
        return;

    if (auto* control = control_node(); control)
        control->handle_associated_label_mousedown({});

    m_tracking_mouse = true;
}

void Label::handle_mouseup_on_label(Badge<TextNode>, const Gfx::IntPoint& position, unsigned button)
{
    if (!m_tracking_mouse || button != GUI::MouseButton::Left)
        return;

    // NOTE: Changing the checked state of the DOM node may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protect = *this;

    if (auto* control = control_node(); control) {
        bool is_inside_control = enclosing_int_rect(control->absolute_rect()).contains(position);
        bool is_inside_label = enclosing_int_rect(absolute_rect()).contains(position);

        if (is_inside_control || is_inside_label)
            control->handle_associated_label_mouseup({});
    }

    m_tracking_mouse = false;
}

void Label::handle_mousemove_on_label(Badge<TextNode>, const Gfx::IntPoint& position, unsigned)
{
    if (!m_tracking_mouse)
        return;

    if (auto* control = control_node(); control) {
        bool is_inside_control = enclosing_int_rect(control->absolute_rect()).contains(position);
        bool is_inside_label = enclosing_int_rect(absolute_rect()).contains(position);

        control->handle_associated_label_mousemove({}, is_inside_control || is_inside_label);
    }
}

bool Label::is_inside_associated_label(LabelableNode& control, const Gfx::IntPoint& position)
{
    if (auto* label = label_for_control_node(control); label)
        return enclosing_int_rect(label->absolute_rect()).contains(position);
    return false;
}

bool Label::is_associated_label_hovered(LabelableNode& control)
{
    if (auto* label = label_for_control_node(control); label) {
        if (label->document().hovered_node() == &label->dom_node())
            return true;

        if (auto* child = label->first_child_of_type<TextNode>(); child)
            return label->document().hovered_node() == &child->dom_node();
    }

    return false;
}

Label* Label::label_for_control_node(LabelableNode& control)
{
    Label* label = nullptr;

    if (!control.document().layout_node())
        return label;

    String id = control.dom_node().attribute(HTML::AttributeNames::id);
    if (id.is_empty())
        return label;

    control.document().layout_node()->for_each_in_subtree_of_type<Label>([&](auto& node) {
        if (node.dom_node().for_() == id) {
            label = &node;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    // FIXME: The spec also allows for associating a label with a labelable node by putting the
    //        labelable node inside the label.
    return label;
}

LabelableNode* Label::control_node()
{
    LabelableNode* control = nullptr;

    if (!document().layout_node())
        return control;

    String for_ = dom_node().for_();
    if (for_.is_empty())
        return control;

    document().layout_node()->for_each_in_subtree_of_type<LabelableNode>([&](auto& node) {
        if (node.dom_node().attribute(HTML::AttributeNames::id) == for_) {
            control = &node;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    // FIXME: The spec also allows for associating a label with a labelable node by putting the
    //        labelable node inside the label.
    return control;
}

}
