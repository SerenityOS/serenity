/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/InlineNode.h>

namespace Web::Layout {

InlineNode::InlineNode(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
    : Layout::NodeWithStyleAndBoxModelMetrics(document, &element, move(style))
{
    set_inline(true);
}

InlineNode::~InlineNode()
{
}

void InlineNode::split_into_lines(InlineFormattingContext& context, LayoutMode layout_mode)
{
    auto& containing_block = context.context_box();

    if (!computed_values().padding().left.is_undefined_or_auto()) {
        float padding_left = computed_values().padding().left.resolved(CSS::Length::make_px(0), *this, containing_block.width()).to_px(*this);
        containing_block.ensure_last_line_box().add_fragment(*this, 0, 0, padding_left, 0, LineBoxFragment::Type::Leading);
    }

    NodeWithStyleAndBoxModelMetrics::split_into_lines(context, layout_mode);

    if (!computed_values().padding().right.is_undefined_or_auto()) {
        float padding_right = computed_values().padding().right.resolved(CSS::Length::make_px(0), *this, containing_block.width()).to_px(*this);
        containing_block.ensure_last_line_box().add_fragment(*this, 0, 0, padding_right, 0, LineBoxFragment::Type::Trailing);
    }
}

void InlineNode::paint_fragment(PaintContext& context, const LineBoxFragment& fragment, PaintPhase phase) const
{
    auto& painter = context.painter();

    if (phase == PaintPhase::Background) {
        painter.fill_rect(enclosing_int_rect(fragment.absolute_rect()), computed_values().background_color());
    }
}

}
