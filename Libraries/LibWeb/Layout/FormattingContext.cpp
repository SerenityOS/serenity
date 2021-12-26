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
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Layout/TableBox.h>
#include <LibWeb/Layout/TableCellBox.h>
#include <LibWeb/Layout/TableFormattingContext.h>
#include <LibWeb/Layout/TableRowBox.h>

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
    if (is<TableCellBox>(box))
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
        context.run(box, layout_mode);
        return;
    }
    if (is<TableBox>(box)) {
        TableFormattingContext context(box, this);
        context.run(box, layout_mode);
    } else if (box.children_are_inline()) {
        InlineFormattingContext context(box, this);
        context.run(box, layout_mode);
    } else {
        // FIXME: This needs refactoring!
        ASSERT(is_block_formatting_context());
        run(box, layout_mode);
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
            max_width = max(max_width, child.border_box_width());
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

static Gfx::FloatSize solve_replaced_size_constraint(float w, float h, const ReplacedBox& box)
{
    // 10.4 Minimum and maximum widths: 'min-width' and 'max-width'

    auto& containing_block = *box.containing_block();
    auto specified_min_width = box.computed_values().min_width().resolved_or_zero(box, containing_block.width()).to_px(box);
    auto specified_max_width = box.computed_values().max_width().resolved(CSS::Length::make_px(w), box, containing_block.width()).to_px(box);
    auto specified_min_height = box.computed_values().min_height().resolved_or_auto(box, containing_block.height()).to_px(box);
    auto specified_max_height = box.computed_values().max_height().resolved(CSS::Length::make_px(h), box, containing_block.height()).to_px(box);

    auto min_width = min(specified_min_width, specified_max_width);
    auto max_width = max(specified_min_width, specified_max_width);
    auto min_height = min(specified_min_height, specified_max_height);
    auto max_height = max(specified_min_height, specified_max_height);

    if (w > max_width)
        return { w, max(max_width * h / w, min_height) };
    if (w < min_width)
        return { max_width, min(min_width * h / w, max_height) };
    if (h > max_height)
        return { max(max_height * w / h, min_width), max_height };
    if (h < min_height)
        return { min(min_height * w / h, max_width), min_height };
    if ((w > max_width && h > max_height) && (max_width / w < max_height / h))
        return { max_width, max(min_height, max_width * h / w) };
    if ((w > max_width && h > max_height) && (max_width / w > max_height / h))
        return { max(min_width, max_height * w / h), max_height };
    if ((w < min_width && h < min_height) && (min_width / w < min_height / h))
        return { min(max_width, min_height * w / h), min_height };
    if ((w < min_width && h < min_height) && (min_width / w > min_height / h))
        return { min_width, min(max_height, min_width * h / w) };
    if (w < min_width && h > max_height)
        return { min_width, max_height };
    if (w > max_width && h < min_height)
        return { max_width, min_height };
    return { w, h };
}

float FormattingContext::tentative_width_for_replaced_element(const ReplacedBox& box, const CSS::Length& width)
{
    auto& containing_block = *box.containing_block();
    auto specified_height = box.computed_values().height().resolved_or_auto(box, containing_block.height());

    float used_width = width.to_px(box);

    // If 'height' and 'width' both have computed values of 'auto' and the element also has an intrinsic width,
    // then that intrinsic width is the used value of 'width'.
    if (specified_height.is_auto() && width.is_auto() && box.has_intrinsic_width()) {
        used_width = box.intrinsic_width();
    }

    // If 'height' and 'width' both have computed values of 'auto' and the element has no intrinsic width,
    // but does have an intrinsic height and intrinsic ratio;
    // or if 'width' has a computed value of 'auto',
    // 'height' has some other computed value, and the element does have an intrinsic ratio; then the used value of 'width' is:
    //
    //     (used height) * (intrinsic ratio)
    else if ((specified_height.is_auto() && width.is_auto() && !box.has_intrinsic_width() && box.has_intrinsic_height() && box.has_intrinsic_ratio()) || (width.is_auto() && box.has_intrinsic_ratio())) {
        used_width = compute_height_for_replaced_element(box) * box.intrinsic_ratio();
    }

    else if (width.is_auto() && box.has_intrinsic_width()) {
        used_width = box.intrinsic_width();
    }

    else if (width.is_auto()) {
        used_width = 300;
    }

    return used_width;
}

void FormattingContext::compute_width_for_absolutely_positioned_element(Box& box)
{
    if (is<ReplacedBox>(box))
        compute_width_for_absolutely_positioned_replaced_element(downcast<ReplacedBox>(box));
    else
        compute_width_for_absolutely_positioned_non_replaced_element(box);
}

void FormattingContext::compute_height_for_absolutely_positioned_element(Box& box)
{
    if (is<ReplacedBox>(box))
        compute_height_for_absolutely_positioned_replaced_element(downcast<ReplacedBox>(box));
    else
        compute_height_for_absolutely_positioned_non_replaced_element(box);
}

float FormattingContext::compute_width_for_replaced_element(const ReplacedBox& box)
{
    // 10.3.4 Block-level, replaced elements in normal flow...
    // 10.3.2 Inline, replaced elements

    auto zero_value = CSS::Length::make_px(0);
    auto& containing_block = *box.containing_block();

    auto margin_left = box.computed_values().margin().left.resolved_or_zero(box, containing_block.width());
    auto margin_right = box.computed_values().margin().right.resolved_or_zero(box, containing_block.width());

    // A computed value of 'auto' for 'margin-left' or 'margin-right' becomes a used value of '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    auto specified_width = box.computed_values().width().resolved_or_auto(box, containing_block.width());

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = tentative_width_for_replaced_element(box, specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = box.computed_values().max_width().resolved_or_auto(box, containing_block.width());
    if (!specified_max_width.is_auto()) {
        if (used_width > specified_max_width.to_px(box)) {
            used_width = tentative_width_for_replaced_element(box, specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = box.computed_values().min_width().resolved_or_auto(box, containing_block.width());
    if (!specified_min_width.is_auto()) {
        if (used_width < specified_min_width.to_px(box)) {
            used_width = tentative_width_for_replaced_element(box, specified_min_width);
        }
    }

    return used_width;
}

float FormattingContext::tentative_height_for_replaced_element(const ReplacedBox& box, const CSS::Length& height)
{
    auto& containing_block = *box.containing_block();
    auto specified_width = box.computed_values().width().resolved_or_auto(box, containing_block.width());

    float used_height = height.to_px(box);

    // If 'height' and 'width' both have computed values of 'auto' and the element also has
    // an intrinsic height, then that intrinsic height is the used value of 'height'.
    if (specified_width.is_auto() && height.is_auto() && box.has_intrinsic_height())
        used_height = box.intrinsic_height();
    else if (height.is_auto() && box.has_intrinsic_ratio())
        used_height = compute_width_for_replaced_element(box) / box.intrinsic_ratio();
    else if (height.is_auto() && box.has_intrinsic_height())
        used_height = box.intrinsic_height();
    else if (height.is_auto())
        used_height = 150;

    return used_height;
}

float FormattingContext::compute_height_for_replaced_element(const ReplacedBox& box)
{
    // 10.6.2 Inline replaced elements, block-level replaced elements in normal flow,
    // 'inline-block' replaced elements in normal flow and floating replaced elements

    auto& containing_block = *box.containing_block();
    auto specified_width = box.computed_values().width().resolved_or_auto(box, containing_block.width());
    auto specified_height = box.computed_values().height().resolved_or_auto(box, containing_block.height());

    float used_height = tentative_height_for_replaced_element(box, specified_height);

    if (specified_width.is_auto() && specified_height.is_auto() && box.has_intrinsic_ratio()) {
        float w = tentative_width_for_replaced_element(box, specified_width);
        float h = used_height;
        used_height = solve_replaced_size_constraint(w, h, box).height();
    }

    return used_height;
}

void FormattingContext::compute_width_for_absolutely_positioned_non_replaced_element(Box& box)
{
    auto& containing_block = *box.containing_block();
    auto& computed_values = box.computed_values();
    auto zero_value = CSS::Length::make_px(0);

    auto margin_left = CSS::Length::make_auto();
    auto margin_right = CSS::Length::make_auto();
    const auto border_left = computed_values.border_left().width;
    const auto border_right = computed_values.border_right().width;
    const auto padding_left = computed_values.padding().left.resolved_or_zero(box, containing_block.width());
    const auto padding_right = computed_values.padding().right.resolved_or_zero(box, containing_block.width());

    auto try_compute_width = [&](const auto& a_width) {
        margin_left = computed_values.margin().left.resolved_or_zero(box, containing_block.width());
        margin_right = computed_values.margin().right.resolved_or_zero(box, containing_block.width());

        auto left = computed_values.offset().left.resolved_or_auto(box, containing_block.width());
        auto right = computed_values.offset().right.resolved_or_auto(box, containing_block.width());
        auto width = a_width;

        auto solve_for_left = [&] {
            return CSS::Length(containing_block.width() - margin_left.to_px(box) - border_left - padding_left.to_px(box) - width.to_px(box) - padding_right.to_px(box) - border_right - margin_right.to_px(box) - right.to_px(box), CSS::Length::Type::Px);
        };

        auto solve_for_width = [&] {
            return CSS::Length(containing_block.width() - left.to_px(box) - margin_left.to_px(box) - border_left - padding_left.to_px(box) - padding_right.to_px(box) - border_right - margin_right.to_px(box) - right.to_px(box), CSS::Length::Type::Px);
        };

        auto solve_for_right = [&] {
            return CSS::Length(containing_block.width() - left.to_px(box) - margin_left.to_px(box) - border_left - padding_left.to_px(box) - width.to_px(box) - padding_right.to_px(box) - border_right - margin_right.to_px(box), CSS::Length::Type::Px);
        };

        // If all three of 'left', 'width', and 'right' are 'auto':
        if (left.is_auto() && width.is_auto() && right.is_auto()) {
            // First set any 'auto' values for 'margin-left' and 'margin-right' to 0.
            if (margin_left.is_auto())
                margin_left = CSS::Length::make_px(0);
            if (margin_right.is_auto())
                margin_right = CSS::Length::make_px(0);
            // Then, if the 'direction' property of the element establishing the static-position containing block
            // is 'ltr' set 'left' to the static position and apply rule number three below;
            // otherwise, set 'right' to the static position and apply rule number one below.
            // FIXME: This is very hackish.
            left = CSS::Length::make_px(0);
            goto Rule3;
        }

        if (!left.is_auto() && !width.is_auto() && !right.is_auto()) {
            // FIXME: This should be solved in a more complicated way.
            return width;
        }

        if (margin_left.is_auto())
            margin_left = CSS::Length::make_px(0);
        if (margin_right.is_auto())
            margin_right = CSS::Length::make_px(0);

        // 1. 'left' and 'width' are 'auto' and 'right' is not 'auto',
        //    then the width is shrink-to-fit. Then solve for 'left'
        if (left.is_auto() && width.is_auto() && !right.is_auto()) {
            auto result = calculate_shrink_to_fit_widths(box);
            solve_for_left();
            auto available_width = solve_for_width();
            width = CSS::Length(min(max(result.preferred_minimum_width, available_width.to_px(box)), result.preferred_width), CSS::Length::Type::Px);
        }

        // 2. 'left' and 'right' are 'auto' and 'width' is not 'auto',
        //    then if the 'direction' property of the element establishing
        //    the static-position containing block is 'ltr' set 'left'
        //    to the static position, otherwise set 'right' to the static position.
        //    Then solve for 'left' (if 'direction is 'rtl') or 'right' (if 'direction' is 'ltr').
        else if (left.is_auto() && right.is_auto() && !width.is_auto()) {
            // FIXME: Check direction
            // FIXME: Use the static-position containing block
            left = zero_value;
            right = solve_for_right();
        }

        // 3. 'width' and 'right' are 'auto' and 'left' is not 'auto',
        //    then the width is shrink-to-fit. Then solve for 'right'
        else if (width.is_auto() && right.is_auto() && !left.is_auto()) {
        Rule3:
            auto result = calculate_shrink_to_fit_widths(box);
            auto available_width = solve_for_width();
            width = CSS::Length(min(max(result.preferred_minimum_width, available_width.to_px(box)), result.preferred_width), CSS::Length::Type::Px);
            right = solve_for_right();
        }

        // 4. 'left' is 'auto', 'width' and 'right' are not 'auto', then solve for 'left'
        else if (left.is_auto() && !width.is_auto() && !right.is_auto()) {
            left = solve_for_left();
        }

        // 5. 'width' is 'auto', 'left' and 'right' are not 'auto', then solve for 'width'
        else if (width.is_auto() && !left.is_auto() && !right.is_auto()) {
            width = solve_for_width();
        }

        // 6. 'right' is 'auto', 'left' and 'width' are not 'auto', then solve for 'right'
        else if (right.is_auto() && !left.is_auto() && !width.is_auto()) {
            right = solve_for_right();
        }

        return width;
    };

    auto specified_width = computed_values.width().resolved_or_auto(box, containing_block.width());

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = computed_values.max_width().resolved_or_auto(box, containing_block.width());
    if (!specified_max_width.is_auto()) {
        if (used_width.to_px(box) > specified_max_width.to_px(box)) {
            used_width = try_compute_width(specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = computed_values.min_width().resolved_or_auto(box, containing_block.width());
    if (!specified_min_width.is_auto()) {
        if (used_width.to_px(box) < specified_min_width.to_px(box)) {
            used_width = try_compute_width(specified_min_width);
        }
    }

    box.set_width(used_width.to_px(box));

    box.box_model().margin.left = margin_left.to_px(box);
    box.box_model().margin.right = margin_right.to_px(box);
    box.box_model().border.left = border_left;
    box.box_model().border.right = border_right;
    box.box_model().padding.left = padding_left.to_px(box);
    box.box_model().padding.right = padding_right.to_px(box);
}

void FormattingContext::compute_width_for_absolutely_positioned_replaced_element(ReplacedBox& box)
{
    // 10.3.8 Absolutely positioned, replaced elements
    // The used value of 'width' is determined as for inline replaced elements.
    box.prepare_for_replaced_layout();
    box.set_width(compute_width_for_replaced_element(box));
}

void FormattingContext::compute_height_for_absolutely_positioned_non_replaced_element(Box& box)
{
    auto& computed_values = box.computed_values();
    auto& containing_block = *box.containing_block();

    CSS::Length specified_height;

    if (computed_values.height().is_percentage() && !containing_block.computed_values().height().is_absolute()) {
        specified_height = CSS::Length::make_auto();
    } else {
        specified_height = computed_values.height().resolved_or_auto(box, containing_block.height());
    }

    auto specified_max_height = computed_values.max_height().resolved_or_auto(box, containing_block.height());

    box.box_model().margin.top = computed_values.margin().top.resolved_or_zero(box, containing_block.width()).to_px(box);
    box.box_model().margin.bottom = computed_values.margin().bottom.resolved_or_zero(box, containing_block.width()).to_px(box);
    box.box_model().border.top = computed_values.border_top().width;
    box.box_model().border.bottom = computed_values.border_bottom().width;
    box.box_model().padding.top = computed_values.padding().top.resolved_or_zero(box, containing_block.width()).to_px(box);
    box.box_model().padding.bottom = computed_values.padding().bottom.resolved_or_zero(box, containing_block.width()).to_px(box);

    if (!specified_height.is_auto()) {
        float used_height = specified_height.to_px(box);
        if (!specified_max_height.is_auto())
            used_height = min(used_height, specified_max_height.to_px(box));
        box.set_height(used_height);
    }
}

void FormattingContext::layout_absolutely_positioned_element(Box& box)
{
    auto& containing_block = context_box();
    auto& box_model = box.box_model();

    auto specified_width = box.computed_values().width().resolved_or_auto(box, containing_block.width());

    compute_width_for_absolutely_positioned_element(box);
    layout_inside(box, LayoutMode::Default);
    compute_height_for_absolutely_positioned_element(box);

    box_model.margin.left = box.computed_values().margin().left.resolved_or_auto(box, containing_block.width()).to_px(box);
    box_model.margin.top = box.computed_values().margin().top.resolved_or_auto(box, containing_block.height()).to_px(box);
    box_model.margin.right = box.computed_values().margin().right.resolved_or_auto(box, containing_block.width()).to_px(box);
    box_model.margin.bottom = box.computed_values().margin().bottom.resolved_or_auto(box, containing_block.height()).to_px(box);

    box_model.border.left = box.computed_values().border_left().width;
    box_model.border.right = box.computed_values().border_right().width;
    box_model.border.top = box.computed_values().border_top().width;
    box_model.border.bottom = box.computed_values().border_bottom().width;

    box_model.offset.left = box.computed_values().offset().left.resolved_or_auto(box, containing_block.width()).to_px(box);
    box_model.offset.top = box.computed_values().offset().top.resolved_or_auto(box, containing_block.height()).to_px(box);
    box_model.offset.right = box.computed_values().offset().right.resolved_or_auto(box, containing_block.width()).to_px(box);
    box_model.offset.bottom = box.computed_values().offset().bottom.resolved_or_auto(box, containing_block.height()).to_px(box);

    if (box.computed_values().offset().left.is_auto() && specified_width.is_auto() && box.computed_values().offset().right.is_auto()) {
        if (box.computed_values().margin().left.is_auto())
            box_model.margin.left = 0;
        if (box.computed_values().margin().right.is_auto())
            box_model.margin.right = 0;
    }

    Gfx::FloatPoint used_offset;

    if (!box.computed_values().offset().left.is_auto()) {
        float x_offset = box_model.offset.left
            + box_model.border_box().left;
        used_offset.set_x(x_offset + box_model.margin.left);
    } else if (!box.computed_values().offset().right.is_auto()) {
        float x_offset = 0
            - box_model.offset.right
            - box_model.border_box().right;
        used_offset.set_x(containing_block.width() + x_offset - box.width() - box_model.margin.right);
    } else {
        float x_offset = box_model.margin_box().left;
        used_offset.set_x(x_offset);
    }

    if (!box.computed_values().offset().top.is_auto()) {
        float y_offset = box_model.offset.top
            + box_model.border_box().top;
        used_offset.set_y(y_offset + box_model.margin.top);
    } else if (!box.computed_values().offset().bottom.is_auto()) {
        float y_offset = 0
            - box_model.offset.bottom
            - box_model.border_box().bottom;
        used_offset.set_y(containing_block.height() + y_offset - box.height() - box_model.margin.bottom);
    } else {
        float y_offset = box_model.margin_box().top;
        used_offset.set_y(y_offset);
    }

    box.set_offset(used_offset);
}

void FormattingContext::compute_height_for_absolutely_positioned_replaced_element(ReplacedBox& box)
{
    // FIXME: Implement this.
    return compute_height_for_absolutely_positioned_non_replaced_element(box);
}

}
