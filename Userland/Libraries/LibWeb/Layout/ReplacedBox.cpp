/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/ReplacedBox.h>

namespace Web::Layout {

ReplacedBox::ReplacedBox(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
    : Box(document, &element, move(style))
{
    // FIXME: Allow non-inline replaced elements.
    set_inline(true);
}

ReplacedBox::~ReplacedBox()
{
}

void ReplacedBox::split_into_lines(InlineFormattingContext& context, LayoutMode layout_mode)
{
    auto& containing_block = context.containing_block();

    prepare_for_replaced_layout();
    context.dimension_box_on_line(*this, layout_mode);

    auto* line_box = &containing_block.ensure_last_line_box();
    if (line_box->width() > 0 && line_box->width() + width() > context.available_width_at_line(containing_block.line_boxes().size() - 1))
        line_box = &containing_block.add_line_box();
    line_box->add_fragment(*this, 0, 0, width(), height());
}

}
