/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FlexFormattingContext.h>

namespace Web::Layout {

FlexFormattingContext::FlexFormattingContext(Box& context_box, FormattingContext* parent)
    : FormattingContext(context_box, parent)
{
}

FlexFormattingContext::~FlexFormattingContext()
{
}

void FlexFormattingContext::run(Box& box, LayoutMode layout_mode)
{
    // FIXME: This is *extremely* naive and only supports flex items laid out on a single line.

    auto flex_direction = box.computed_values().flex_direction();
    bool horizontal = flex_direction == CSS::FlexDirection::Row || flex_direction == CSS::FlexDirection::RowReverse;

    auto available_width = box.containing_block()->width();

    // First, compute the size of each flex item.
    box.for_each_child_of_type<Box>([&](Box& child_box) {
        auto shrink_to_fit_result = calculate_shrink_to_fit_widths(child_box);
        auto shrink_to_fit_width = min(max(shrink_to_fit_result.preferred_minimum_width, available_width), shrink_to_fit_result.preferred_width);
        child_box.set_width(shrink_to_fit_width);
        layout_inside(child_box, layout_mode);
    });

    // Then, place the items on a vertical or horizontal line.
    float x = 0;
    float y = 0;
    float tallest_child_height = 0;
    float widest_child_width = 0;
    box.for_each_child_of_type<Box>([&](Box& child_box) {
        child_box.set_offset(x, y);
        tallest_child_height = max(tallest_child_height, child_box.height());
        widest_child_width = max(widest_child_width, child_box.width());
        if (horizontal)
            x += child_box.margin_box_width();
        else
            y += child_box.margin_box_height();
    });

    // Then, compute the height of the entire flex container.
    // FIXME: This is not correct height calculation..
    if (horizontal) {
        box.set_height(tallest_child_height);
        box.set_width(x);
    } else {
        box.set_width(widest_child_width);
        box.set_height(y);
    }
}

}
