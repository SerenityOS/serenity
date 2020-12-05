/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibWeb/Dump.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/TableFormattingContext.h>

namespace Web::Layout {

FormattingContext::FormattingContext(Box& context_box, FormattingContext* parent)
    : m_parent(parent)
    , m_context_box(&context_box)
{
}

FormattingContext::~FormattingContext()
{
}

bool FormattingContext::creates_block_formatting_context(const Box& box)
{
    if (box.is_root_element())
        return true;
    if (box.is_floating())
        return true;
    if (box.is_absolutely_positioned())
        return true;
    if (box.is_inline_block())
        return true;
    if (box.is_table_cell())
        return true;
    // FIXME: table-caption
    // FIXME: anonymous table cells
    // FIXME: Block elements where overflow has a value other than visible and clip.
    // FIXME: display: flow-root
    // FIXME: Elements with contain: layout, content, or paint.
    // FIXME: flex
    // FIXME: grid
    // FIXME: multicol
    // FIXME: column-span: all
    return false;
}

void FormattingContext::layout_inside(Box& box, LayoutMode layout_mode)
{
    if (creates_block_formatting_context(box)) {
        BlockFormattingContext context(box, this);
        context.run(layout_mode);
        return;
    }
    if (box.is_table()) {
        TableFormattingContext context(box, this);
        context.run(layout_mode);
    } else if (box.children_are_inline()) {
        InlineFormattingContext context(box, this);
        context.run(layout_mode);
    } else {
        // FIXME: This needs refactoring!
        ASSERT(is_block_formatting_context());
        auto& old_box = context_box();
        set_context_box(box);
        run(layout_mode);
        set_context_box(old_box);
    }
}

static float greatest_child_width(const Box& box)
{
    float max_width = 0;
    if (box.children_are_inline()) {
        for (auto& child : box.line_boxes()) {
            max_width = max(max_width, child.width());
        }
    } else {
        box.for_each_child_of_type<Box>([&](auto& child) {
            max_width = max(max_width, child.width());
        });
    }
    return max_width;
}

FormattingContext::ShrinkToFitResult FormattingContext::calculate_shrink_to_fit_widths(Box& box)
{
    // Calculate the preferred width by formatting the content without breaking lines
    // other than where explicit line breaks occur.
    layout_inside(box, LayoutMode::OnlyRequiredLineBreaks);
    float preferred_width = greatest_child_width(box);

    // Also calculate the preferred minimum width, e.g., by trying all possible line breaks.
    // CSS 2.2 does not define the exact algorithm.

    layout_inside(box, LayoutMode::AllPossibleLineBreaks);
    float preferred_minimum_width = greatest_child_width(box);

    return { preferred_width, preferred_minimum_width };
}

}
