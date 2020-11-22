/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/Painter.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/LayoutBlock.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutInline.h>
#include <LibWeb/Layout/LayoutReplaced.h>
#include <LibWeb/Layout/LayoutText.h>
#include <LibWeb/Layout/LayoutWidget.h>
#include <math.h>

namespace Web {

LayoutBlock::LayoutBlock(DOM::Document& document, DOM::Node* node, NonnullRefPtr<CSS::StyleProperties> style)
    : LayoutBox(document, node, move(style))
{
}

LayoutBlock::~LayoutBlock()
{
}

LayoutNode& LayoutBlock::inline_wrapper()
{
    if (!last_child() || !last_child()->is_block() || last_child()->dom_node() != nullptr) {
        append_child(adopt(*new LayoutBlock(document(), nullptr, style_for_anonymous_block())));
        last_child()->set_children_are_inline(true);
    }
    return *last_child();
}

void LayoutBlock::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    LayoutBox::paint(context, phase);

    // FIXME: Inline backgrounds etc.
    if (phase == PaintPhase::Foreground) {
        if (children_are_inline()) {
            for (auto& line_box : m_line_boxes) {
                for (auto& fragment : line_box.fragments()) {
                    if (context.should_show_line_box_borders())
                        context.painter().draw_rect(enclosing_int_rect(fragment.absolute_rect()), Color::Green);
                    fragment.paint(context);
                }
            }
        }
    }

    if (phase == PaintPhase::FocusOutline) {
        if (children_are_inline()) {
            for (auto& line_box : m_line_boxes) {
                for (auto& fragment : line_box.fragments()) {
                    auto* node = fragment.layout_node().dom_node();
                    if (!node)
                        continue;
                    auto* parent = node->parent_element();
                    if (!parent)
                        continue;
                    if (parent->is_focused())
                        context.painter().draw_rect(enclosing_int_rect(fragment.absolute_rect()), context.palette().focus_outline());
                }
            }
        }
    }
}

HitTestResult LayoutBlock::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    if (!children_are_inline())
        return LayoutBox::hit_test(position, type);

    HitTestResult last_good_candidate;
    for (auto& line_box : m_line_boxes) {
        for (auto& fragment : line_box.fragments()) {
            if (is<LayoutBox>(fragment.layout_node()) && downcast<LayoutBox>(fragment.layout_node()).stacking_context())
                continue;
            if (enclosing_int_rect(fragment.absolute_rect()).contains(position)) {
                if (fragment.layout_node().is_block())
                    return downcast<LayoutBlock>(fragment.layout_node()).hit_test(position, type);
                return { fragment.layout_node(), fragment.text_index_at(position.x()) };
            }
            if (fragment.absolute_rect().top() <= position.y())
                last_good_candidate = { fragment.layout_node(), fragment.text_index_at(position.x()) };
        }
    }

    if (type == HitTestType::TextCursor && last_good_candidate.layout_node)
        return last_good_candidate;
    return { absolute_rect().contains(position.x(), position.y()) ? this : nullptr };
}

NonnullRefPtr<CSS::StyleProperties> LayoutBlock::style_for_anonymous_block() const
{
    auto new_style = CSS::StyleProperties::create();

    specified_style().for_each_property([&](auto property_id, auto& value) {
        if (CSS::StyleResolver::is_inherited_property(property_id))
            new_style->set_property(property_id, value);
    });

    return new_style;
}

void LayoutBlock::split_into_lines(LayoutBlock& container, LayoutMode layout_mode)
{
    auto* line_box = &container.ensure_last_line_box();
    if (layout_mode != LayoutMode::OnlyRequiredLineBreaks && line_box->width() > 0 && line_box->width() + width() > container.width()) {
        line_box = &container.add_line_box();
    }
    line_box->add_fragment(*this, 0, 0, width(), height());
}

}
