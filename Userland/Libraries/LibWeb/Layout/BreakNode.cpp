/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/BreakNode.h>
#include <LibWeb/Layout/InlineFormattingContext.h>

namespace Web::Layout {

BreakNode::BreakNode(DOM::Document& document, HTML::HTMLBRElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : Layout::NodeWithStyleAndBoxModelMetrics(document, &element, move(style))
{
    set_inline(true);
}

BreakNode::~BreakNode()
{
}

void BreakNode::split_into_lines(InlineFormattingContext& context, LayoutMode)
{
    auto& line_box = context.containing_block().add_line_box();
    line_box.add_fragment(*this, 0, 0, 0, context.containing_block().line_height());
}

void BreakNode::paint(PaintContext&, PaintPhase)
{
}

}
