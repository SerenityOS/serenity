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

    if (!style().padding().left.is_undefined_or_auto()) {
        float padding_left = style().padding().left.resolved(CSS::Length::make_px(0), *this, containing_block.width()).to_px(*this);
        containing_block.ensure_last_line_box().add_fragment(*this, 0, 0, padding_left, 0, LineBoxFragment::Type::Leading);
    }

    NodeWithStyleAndBoxModelMetrics::split_into_lines(context, layout_mode);

    if (!style().padding().right.is_undefined_or_auto()) {
        float padding_right = style().padding().right.resolved(CSS::Length::make_px(0), *this, containing_block.width()).to_px(*this);
        containing_block.ensure_last_line_box().add_fragment(*this, 0, 0, padding_right, 0, LineBoxFragment::Type::Trailing);
    }
}

void InlineNode::paint_fragment(PaintContext& context, const LineBoxFragment& fragment, PaintPhase phase) const
{
    auto& painter = context.painter();

    if (phase == PaintPhase::Background) {
        painter.fill_rect(enclosing_int_rect(fragment.absolute_rect()), style().background_color());
    }
}

}
