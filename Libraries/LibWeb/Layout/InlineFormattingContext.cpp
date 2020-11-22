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

#include <LibWeb/CSS/Length.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/LayoutBlock.h>
#include <LibWeb/Layout/LayoutBox.h>
#include <LibWeb/Layout/LayoutInline.h>
#include <LibWeb/Layout/LayoutReplaced.h>

namespace Web::Layout {

InlineFormattingContext::InlineFormattingContext(LayoutBox& containing_block)
    : FormattingContext(containing_block)
{
}

InlineFormattingContext::~InlineFormattingContext()
{
}

void InlineFormattingContext::run(LayoutMode layout_mode)
{
    auto& containing_block = downcast<LayoutBlock>(context_box());

    ASSERT(containing_block.children_are_inline());
    containing_block.line_boxes().clear();
    containing_block.for_each_child([&](auto& child) {
        ASSERT(child.is_inline());
        if (child.is_absolutely_positioned())
            return;

        child.split_into_lines(containing_block, layout_mode);
    });

    for (auto& line_box : containing_block.line_boxes()) {
        line_box.trim_trailing_whitespace();
    }

    // If there's an empty line box at the bottom, just remove it instead of giving it height.
    if (!containing_block.line_boxes().is_empty() && containing_block.line_boxes().last().fragments().is_empty())
        containing_block.line_boxes().take_last();

    auto text_align = containing_block.style().text_align();
    float min_line_height = containing_block.specified_style().line_height(containing_block);
    float line_spacing = min_line_height - containing_block.specified_style().font().glyph_height();
    float content_height = 0;
    float max_linebox_width = 0;

    for (auto& line_box : containing_block.line_boxes()) {
        float max_height = min_line_height;
        for (auto& fragment : line_box.fragments()) {
            max_height = max(max_height, fragment.height());
        }

        float x_offset = 0;
        float excess_horizontal_space = (float)containing_block.width() - line_box.width();

        switch (text_align) {
        case CSS::TextAlign::Center:
        case CSS::TextAlign::VendorSpecificCenter:
            x_offset += excess_horizontal_space / 2;
            break;
        case CSS::TextAlign::Right:
            x_offset += excess_horizontal_space;
            break;
        case CSS::TextAlign::Left:
        case CSS::TextAlign::Justify:
        default:
            break;
        }

        float excess_horizontal_space_including_whitespace = excess_horizontal_space;
        int whitespace_count = 0;
        if (text_align == CSS::TextAlign::Justify) {
            for (auto& fragment : line_box.fragments()) {
                if (fragment.is_justifiable_whitespace()) {
                    ++whitespace_count;
                    excess_horizontal_space_including_whitespace += fragment.width();
                }
            }
        }

        float justified_space_width = whitespace_count ? (excess_horizontal_space_including_whitespace / (float)whitespace_count) : 0;

        for (size_t i = 0; i < line_box.fragments().size(); ++i) {
            auto& fragment = line_box.fragments()[i];

            // Vertically align everyone's bottom to the line.
            // FIXME: Support other kinds of vertical alignment.
            fragment.set_offset({ roundf(x_offset + fragment.offset().x()), content_height + (max_height - fragment.height()) - (line_spacing / 2) });

            if (text_align == CSS::TextAlign::Justify
                && fragment.is_justifiable_whitespace()
                && fragment.width() != justified_space_width) {
                float diff = justified_space_width - fragment.width();
                fragment.set_width(justified_space_width);
                // Shift subsequent sibling fragments to the right to adjust for change in width.
                for (size_t j = i + 1; j < line_box.fragments().size(); ++j) {
                    auto offset = line_box.fragments()[j].offset();
                    offset.move_by(diff, 0);
                    line_box.fragments()[j].set_offset(offset);
                }
            }

            if (fragment.layout_node().is_box())
                dimension_box_on_line(const_cast<LayoutBox&>(downcast<LayoutBox>(fragment.layout_node())), layout_mode);

            float final_line_box_width = 0;
            for (auto& fragment : line_box.fragments())
                final_line_box_width += fragment.width();
            line_box.m_width = final_line_box_width;

            max_linebox_width = max(max_linebox_width, final_line_box_width);
        }

        content_height += max_height;
    }

    if (layout_mode != LayoutMode::Default) {
        containing_block.set_width(max_linebox_width);
    }

    containing_block.set_height(content_height);
}

void InlineFormattingContext::dimension_box_on_line(LayoutBox& box, LayoutMode layout_mode)
{
    auto& containing_block = downcast<LayoutBlock>(context_box());

    if (box.is_replaced()) {
        auto& replaced = const_cast<LayoutReplaced&>(downcast<LayoutReplaced>(box));
        replaced.set_width(replaced.calculate_width());
        replaced.set_height(replaced.calculate_height());
        return;
    }

    if (box.is_inline_block()) {
        auto& inline_block = const_cast<LayoutBlock&>(downcast<LayoutBlock>(box));

        if (inline_block.style().width().is_undefined_or_auto()) {
            auto result = calculate_shrink_to_fit_widths(inline_block);

            // FIXME: (10.3.5) find the available width: in this case, this is the width of the containing
            //        block minus the used values of 'margin-left', 'border-left-width', 'padding-left',
            //        'padding-right', 'border-right-width', 'margin-right', and the widths of any
            //        relevant scroll bars.
            auto available_width = containing_block.width();

            auto width = min(max(result.preferred_minimum_width, available_width), result.preferred_width);
            inline_block.set_width(width);
        } else {
            inline_block.set_width(inline_block.style().width().to_px(inline_block));
        }

        FormattingContext::layout_inside(inline_block, layout_mode);

        if (inline_block.style().height().is_undefined_or_auto()) {
            // FIXME: (10.6.6) If 'height' is 'auto', the height depends on the element's descendants per 10.6.7.
        } else {
            inline_block.set_height(inline_block.style().height().to_px(inline_block));
        }
        return;
    }

    // Non-replaced, non-inline-block, box on a line!?
    // I don't think we should be here. Dump the box tree so we can take a look at it.
    dbgln("FIXME: I've been asked to dimension a non-replaced, non-inline-block box on a line:");
    dump_tree(box);
}

}
