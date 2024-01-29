/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/Painting/ViewportPaintable.h>

namespace Web::Painting {

JS::NonnullGCPtr<ViewportPaintable> ViewportPaintable::create(Layout::Viewport const& layout_viewport)
{
    return layout_viewport.heap().allocate_without_realm<ViewportPaintable>(layout_viewport);
}

ViewportPaintable::ViewportPaintable(Layout::Viewport const& layout_viewport)
    : PaintableWithLines(layout_viewport)
{
}

ViewportPaintable::~ViewportPaintable() = default;

void ViewportPaintable::build_stacking_context_tree_if_needed()
{
    if (stacking_context())
        return;
    build_stacking_context_tree();
}

void ViewportPaintable::build_stacking_context_tree()
{
    set_stacking_context(make<StackingContext>(*this, nullptr, 0));

    size_t index_in_tree_order = 1;
    for_each_in_subtree([&](Paintable const& paintable) {
        const_cast<Paintable&>(paintable).invalidate_stacking_context();
        if (!paintable.layout_node().establishes_stacking_context()) {
            VERIFY(!paintable.stacking_context());
            return TraversalDecision::Continue;
        }
        auto* parent_context = const_cast<Paintable&>(paintable).enclosing_stacking_context();
        VERIFY(parent_context);
        const_cast<Paintable&>(paintable).set_stacking_context(make<Painting::StackingContext>(const_cast<Paintable&>(paintable), parent_context, index_in_tree_order++));
        return TraversalDecision::Continue;
    });

    stacking_context()->sort();
}

void ViewportPaintable::paint_all_phases(PaintContext& context)
{
    build_stacking_context_tree_if_needed();
    context.recording_painter().translate(-context.device_viewport_rect().location().to_type<int>());
    stacking_context()->paint(context);
}

void ViewportPaintable::assign_scroll_frame_ids(HashMap<Painting::PaintableBox const*, ScrollFrame>& scroll_frames) const
{
    i32 next_id = 0;
    // Collect scroll frames with their offsets (accumulated offset for nested scroll frames).
    for_each_in_subtree_of_type<PaintableBox>([&](auto const& paintable_box) {
        if (paintable_box.has_scrollable_overflow()) {
            auto offset = paintable_box.scroll_offset();
            auto ancestor = paintable_box.containing_block();
            while (ancestor) {
                if (ancestor->paintable()->is_paintable_box() && static_cast<PaintableBox const*>(ancestor->paintable())->has_scrollable_overflow())
                    offset.translate_by(static_cast<PaintableBox const*>(ancestor->paintable())->scroll_offset());
                ancestor = ancestor->containing_block();
            }
            scroll_frames.set(&paintable_box, { .id = next_id++, .offset = -offset });
        }
        return TraversalDecision::Continue;
    });

    // Assign scroll frame id to all paintables contained in a scroll frame.
    for_each_in_subtree([&](auto const& paintable) {
        for (auto block = paintable.containing_block(); block; block = block->containing_block()) {
            auto const& block_paintable_box = *block->paintable_box();
            if (auto scroll_frame_id = scroll_frames.get(&block_paintable_box); scroll_frame_id.has_value()) {
                if (paintable.is_paintable_box()) {
                    auto const& paintable_box = static_cast<PaintableBox const&>(paintable);
                    const_cast<PaintableBox&>(paintable_box).set_scroll_frame_id(scroll_frame_id->id);
                    const_cast<PaintableBox&>(paintable_box).set_enclosing_scroll_frame_offset(scroll_frame_id->offset);
                } else if (paintable.is_inline_paintable()) {
                    auto const& inline_paintable = static_cast<InlinePaintable const&>(paintable);
                    const_cast<InlinePaintable&>(inline_paintable).set_scroll_frame_id(scroll_frame_id->id);
                    const_cast<InlinePaintable&>(inline_paintable).set_enclosing_scroll_frame_offset(scroll_frame_id->offset);
                }
                break;
            }
        }
        return TraversalDecision::Continue;
    });
}

void ViewportPaintable::assign_clip_rectangles(PaintContext const& context)
{
    HashMap<Paintable const*, CSSPixelRect> clip_rects;
    // Calculate clip rects for all boxes that either have hidden overflow or a CSS clip property.
    for_each_in_subtree_of_type<PaintableBox>([&](auto const& paintable_box) {
        auto overflow_x = paintable_box.computed_values().overflow_x();
        auto overflow_y = paintable_box.computed_values().overflow_y();
        // Start from CSS clip property if it exists.
        Optional<CSSPixelRect> clip_rect = paintable_box.get_clip_rect();
        // FIXME: Support overflow clip in one direction only.
        if (overflow_x != CSS::Overflow::Visible && overflow_y != CSS::Overflow::Visible) {
            auto overflow_clip_rect = paintable_box.compute_absolute_padding_rect_with_css_transform_applied();
            for (auto block = &paintable_box.layout_box(); !block->is_viewport(); block = block->containing_block()) {
                auto const& block_paintable_box = *block->paintable_box();
                auto block_overflow_x = block_paintable_box.computed_values().overflow_x();
                auto block_overflow_y = block_paintable_box.computed_values().overflow_y();
                if (block_overflow_x != CSS::Overflow::Visible && block_overflow_y != CSS::Overflow::Visible)
                    overflow_clip_rect.intersect(block_paintable_box.compute_absolute_padding_rect_with_css_transform_applied());
                if (auto css_clip_property_rect = block->paintable_box()->get_clip_rect(); css_clip_property_rect.has_value())
                    overflow_clip_rect.intersect(css_clip_property_rect.value());
            }
            clip_rect = overflow_clip_rect;
        }
        if (clip_rect.has_value())
            clip_rects.set(&paintable_box, *clip_rect);
        return TraversalDecision::Continue;
    });

    // Assign clip rects to all paintable boxes contained by a box with a hidden overflow or a CSS clip property.
    for_each_in_subtree_of_type<PaintableBox>([&](auto const& paintable_box) {
        Optional<CSSPixelRect> clip_rect = paintable_box.get_clip_rect();
        for (auto block = paintable_box.containing_block(); block; block = block->containing_block()) {
            if (auto containing_block_clip_rect = clip_rects.get(block->paintable()); containing_block_clip_rect.has_value()) {
                auto border_radii_data = block->paintable_box()->normalized_border_radii_data(ShrinkRadiiForBorders::Yes);
                CornerRadii corner_radii = border_radii_data.as_corners(context);
                if (corner_radii.has_any_radius()) {
                    // FIXME: Border radii of all boxes in containing block chain should be taken into account instead of just the closest one.
                    const_cast<PaintableBox&>(paintable_box).set_corner_clip_radii(corner_radii);
                }
                clip_rect = *containing_block_clip_rect;
                break;
            }
        }
        const_cast<PaintableBox&>(paintable_box).set_clip_rect(clip_rect);
        return TraversalDecision::Continue;
    });

    // Assign clip rects to all inline paintables contained by a box with hidden overflow or a CSS clip property.
    for_each_in_subtree_of_type<InlinePaintable>([&](auto const& paintable_box) {
        for (auto block = paintable_box.containing_block(); block; block = block->containing_block()) {
            if (auto clip_rect = clip_rects.get(block->paintable()); clip_rect.has_value()) {
                const_cast<InlinePaintable&>(paintable_box).set_clip_rect(clip_rect);
                break;
            }
        }
        return TraversalDecision::Continue;
    });
}

}
