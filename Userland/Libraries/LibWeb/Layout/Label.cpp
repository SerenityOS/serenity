/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Layout/LabelableNode.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Layout {

Label::Label(DOM::Document& document, HTML::HTMLLabelElement* element, NonnullRefPtr<CSS::StyleProperties> style)
    : BlockContainer(document, element, move(style))
{
}

Label::~Label()
{
}

void Label::handle_mousedown_on_label(Badge<TextNode>, const Gfx::IntPoint&, unsigned button)
{
    if (button != GUI::MouseButton::Primary)
        return;

    if (auto* control = labeled_control(); control)
        control->handle_associated_label_mousedown({});

    m_tracking_mouse = true;
}

void Label::handle_mouseup_on_label(Badge<TextNode>, const Gfx::IntPoint& position, unsigned button)
{
    if (!m_tracking_mouse || button != GUI::MouseButton::Primary)
        return;

    // NOTE: Changing the checked state of the DOM node may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protect = *this;

    if (auto* control = labeled_control(); control) {
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

    if (auto* control = labeled_control(); control) {
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

// https://html.spec.whatwg.org/multipage/forms.html#labeled-control
Label* Label::label_for_control_node(LabelableNode& control)
{
    if (!control.document().layout_node())
        return nullptr;

    // The for attribute may be specified to indicate a form control with which the caption is to be associated.
    // If the attribute is specified, the attribute's value must be the ID of a labelable element in the
    // same tree as the label element. If the attribute is specified and there is an element in the tree
    // whose ID is equal to the value of the for attribute, and the first such element in tree order is
    // a labelable element, then that element is the label element's labeled control.
    if (auto id = control.dom_node().attribute(HTML::AttributeNames::id); !id.is_empty()) {
        Label* label = nullptr;

        control.document().layout_node()->for_each_in_inclusive_subtree_of_type<Label>([&](auto& node) {
            if (node.dom_node().for_() == id) {
                label = &node;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });

        if (label)
            return label;
    }

    // If the for attribute is not specified, but the label element has a labelable element descendant,
    // then the first such descendant in tree order is the label element's labeled control.
    return control.first_ancestor_of_type<Label>();
}

// https://html.spec.whatwg.org/multipage/forms.html#labeled-control
LabelableNode* Label::labeled_control()
{
    if (!document().layout_node())
        return nullptr;

    LabelableNode* control = nullptr;

    // The for attribute may be specified to indicate a form control with which the caption is to be associated.
    // If the attribute is specified, the attribute's value must be the ID of a labelable element in the
    // same tree as the label element. If the attribute is specified and there is an element in the tree
    // whose ID is equal to the value of the for attribute, and the first such element in tree order is
    // a labelable element, then that element is the label element's labeled control.
    if (auto for_ = dom_node().for_(); !for_.is_null()) {
        document().layout_node()->for_each_in_inclusive_subtree_of_type<LabelableNode>([&](auto& node) {
            if (node.dom_node().attribute(HTML::AttributeNames::id) == for_) {
                control = &node;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        return control;
    }

    // If the for attribute is not specified, but the label element has a labelable element descendant,
    // then the first such descendant in tree order is the label element's labeled control.
    for_each_in_subtree_of_type<LabelableNode>([&](auto& labelable_node) {
        control = &labelable_node;
        return IterationDecision::Break;
    });
    return control;
}

}
