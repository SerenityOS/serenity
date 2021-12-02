/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Length.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/ReplacedBox.h>

namespace Web::Layout {

InlineFormattingContext::InlineFormattingContext(BlockContainer& containing_block, FormattingContext* parent)
    : FormattingContext(Type::Inline, containing_block, parent)
{
}

InlineFormattingContext::~InlineFormattingContext()
{
}

struct AvailableSpaceForLineInfo {
    float left { 0 };
    float right { 0 };
};

static AvailableSpaceForLineInfo available_space_for_line(const InlineFormattingContext& context, size_t line_index)
{
    if (!context.parent()->is_block_formatting_context())
        return { 0, context.context_box().width() };

    AvailableSpaceForLineInfo info;

    // FIXME: This is a total hack guess since we don't actually know the final y position of lines here!
    float line_height = context.containing_block().line_height();
    float y = (line_index * line_height);

    auto& bfc = static_cast<const BlockFormattingContext&>(*context.parent());

    for (ssize_t i = bfc.left_floating_boxes().size() - 1; i >= 0; --i) {
        auto& floating_box = *bfc.left_floating_boxes().at(i);
        auto rect = floating_box.margin_box_as_relative_rect();
        if (rect.contains_vertically(y)) {
            info.left = rect.right() + 1;
            break;
        }
    }

    info.right = context.containing_block().width();

    for (ssize_t i = bfc.right_floating_boxes().size() - 1; i >= 0; --i) {
        auto& floating_box = *bfc.right_floating_boxes().at(i);
        auto rect = floating_box.margin_box_as_relative_rect();
        if (rect.contains_vertically(y)) {
            info.right = rect.left() - 1;
            break;
        }
    }

    return info;
}

float InlineFormattingContext::available_width_at_line(size_t line_index) const
{
    auto info = available_space_for_line(*this, line_index);
    return info.right - info.left;
}

void InlineFormattingContext::run(Box&, LayoutMode layout_mode)
{
    VERIFY(containing_block().children_are_inline());
    containing_block().line_boxes().clear();
    containing_block().for_each_child([&](auto& child) {
        VERIFY(child.is_inline());
        if (is<Box>(child) && child.is_absolutely_positioned()) {
            layout_absolutely_positioned_element(verify_cast<Box>(child));
            return;
        }

        child.split_into_lines(*this, layout_mode);
    });

    for (auto& line_box : containing_block().line_boxes()) {
        line_box.trim_trailing_whitespace();
    }

    // If there's an empty line box at the bottom, just remove it instead of giving it height.
    if (!containing_block().line_boxes().is_empty() && containing_block().line_boxes().last().fragments().is_empty())
        containing_block().line_boxes().take_last();

    auto text_align = containing_block().computed_values().text_align();
    float min_line_height = containing_block().line_height();
    float content_height = 0;
    float max_linebox_width = 0;

    for (size_t line_index = 0; line_index < containing_block().line_boxes().size(); ++line_index) {
        auto& line_box = containing_block().line_boxes()[line_index];
        float max_height = min_line_height;
        for (auto& fragment : line_box.fragments()) {
            max_height = max(max_height, fragment.height());
        }

        float x_offset = available_space_for_line(*this, line_index).left;

        float excess_horizontal_space = (float)containing_block().width() - line_box.width();

        switch (text_align) {
        case CSS::TextAlign::Center:
        case CSS::TextAlign::LibwebCenter:
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

            if (fragment.type() == LineBoxFragment::Type::Leading || fragment.type() == LineBoxFragment::Type::Trailing) {
                fragment.set_height(max_height);
            }

            // Vertically align everyone's bottom to the line.
            // FIXME: Support other kinds of vertical alignment.
            fragment.set_offset({ roundf(x_offset + fragment.offset().x()), content_height + (max_height - fragment.height()) });

            if (text_align == CSS::TextAlign::Justify
                && fragment.is_justifiable_whitespace()
                && fragment.width() != justified_space_width) {
                float diff = justified_space_width - fragment.width();
                fragment.set_width(justified_space_width);
                // Shift subsequent sibling fragments to the right to adjust for change in width.
                for (size_t j = i + 1; j < line_box.fragments().size(); ++j) {
                    auto offset = line_box.fragments()[j].offset();
                    offset.translate_by(diff, 0);
                    line_box.fragments()[j].set_offset(offset);
                }
            }
        }

        if (!line_box.fragments().is_empty()) {
            float left_edge = line_box.fragments().first().offset().x();
            float right_edge = line_box.fragments().last().offset().x() + line_box.fragments().last().width();
            float final_line_box_width = right_edge - left_edge;
            line_box.m_width = final_line_box_width;
            max_linebox_width = max(max_linebox_width, final_line_box_width);
        }

        content_height += max_height;
    }

    if (layout_mode != LayoutMode::Default) {
        containing_block().set_width(max_linebox_width);
    }

    containing_block().set_height(content_height);
}

void InlineFormattingContext::dimension_box_on_line(Box& box, LayoutMode layout_mode)
{
    if (is<ReplacedBox>(box)) {
        auto& replaced = verify_cast<ReplacedBox>(box);
        replaced.set_width(compute_width_for_replaced_element(replaced));
        replaced.set_height(compute_height_for_replaced_element(replaced));
        return;
    }

    if (box.is_inline_block()) {
        auto& inline_block = const_cast<BlockContainer&>(verify_cast<BlockContainer>(box));

        if (inline_block.computed_values().width().is_undefined_or_auto()) {
            auto result = calculate_shrink_to_fit_widths(inline_block);

            auto margin_left = inline_block.computed_values().margin().left.resolved_or_zero(inline_block, containing_block().width()).to_px(inline_block);
            auto border_left_width = inline_block.computed_values().border_left().width;
            auto padding_left = inline_block.computed_values().padding().left.resolved_or_zero(inline_block, containing_block().width()).to_px(inline_block);

            auto margin_right = inline_block.computed_values().margin().right.resolved_or_zero(inline_block, containing_block().width()).to_px(inline_block);
            auto border_right_width = inline_block.computed_values().border_right().width;
            auto padding_right = inline_block.computed_values().padding().right.resolved_or_zero(inline_block, containing_block().width()).to_px(inline_block);

            auto available_width = containing_block().width()
                - margin_left
                - border_left_width
                - padding_left
                - padding_right
                - border_right_width
                - margin_right;

            auto width = min(max(result.preferred_minimum_width, available_width), result.preferred_width);
            inline_block.set_width(width);
        } else {
            inline_block.set_width(inline_block.computed_values().width().resolved_or_zero(inline_block, containing_block().width()).to_px(inline_block));
        }
        (void)layout_inside(inline_block, layout_mode);

        if (inline_block.computed_values().height().is_undefined_or_auto()) {
            // FIXME: (10.6.6) If 'height' is 'auto', the height depends on the element's descendants per 10.6.7.
        } else {
            inline_block.set_height(inline_block.computed_values().height().resolved_or_zero(inline_block, containing_block().height()).to_px(inline_block));
        }
        return;
    }

    // Non-replaced, non-inline-block, box on a line!?
    // I don't think we should be here. Dump the box tree so we can take a look at it.
    dbgln("FIXME: I've been asked to dimension a non-replaced, non-inline-block box on a line:");
    dump_tree(box);
}

}
