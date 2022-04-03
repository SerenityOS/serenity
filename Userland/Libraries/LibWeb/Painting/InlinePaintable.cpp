/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/BackgroundPainting.h>
#include <LibWeb/Painting/InlinePaintable.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Painting {

NonnullRefPtr<InlinePaintable> InlinePaintable::create(Layout::InlineNode const& layout_node)
{
    return adopt_ref(*new InlinePaintable(layout_node));
}

InlinePaintable::InlinePaintable(Layout::InlineNode const& layout_node)
    : Paintable(layout_node)
{
}

Layout::InlineNode const& InlinePaintable::layout_node() const
{
    return static_cast<Layout::InlineNode const&>(Paintable::layout_node());
}

void InlinePaintable::paint(PaintContext& context, Painting::PaintPhase phase) const
{
    auto& painter = context.painter();

    if (phase == Painting::PaintPhase::Background) {
        auto top_left_border_radius = computed_values().border_top_left_radius();
        auto top_right_border_radius = computed_values().border_top_right_radius();
        auto bottom_right_border_radius = computed_values().border_bottom_right_radius();
        auto bottom_left_border_radius = computed_values().border_bottom_left_radius();
        auto containing_block_position_in_absolute_coordinates = containing_block()->paint_box()->absolute_position();

        for_each_fragment([&](auto const& fragment, bool is_first_fragment, bool is_last_fragment) {
            Gfx::FloatRect absolute_fragment_rect { containing_block_position_in_absolute_coordinates.translated(fragment.offset()), fragment.size() };

            if (is_first_fragment) {
                float extra_start_width = box_model().padding.left;
                absolute_fragment_rect.translate_by(-extra_start_width, 0);
                absolute_fragment_rect.set_width(absolute_fragment_rect.width() + extra_start_width);
            }

            if (is_last_fragment) {
                float extra_end_width = box_model().padding.right;
                absolute_fragment_rect.set_width(absolute_fragment_rect.width() + extra_end_width);
            }

            auto border_radius_data = Painting::normalized_border_radius_data(layout_node(), absolute_fragment_rect, top_left_border_radius, top_right_border_radius, bottom_right_border_radius, bottom_left_border_radius);
            Painting::paint_background(context, layout_node(), absolute_fragment_rect, computed_values().background_color(), &computed_values().background_layers(), border_radius_data);

            if (auto computed_box_shadow = computed_values().box_shadow(); !computed_box_shadow.is_empty()) {
                Vector<Painting::ShadowData> resolved_box_shadow_data;
                resolved_box_shadow_data.ensure_capacity(computed_box_shadow.size());
                for (auto const& layer : computed_box_shadow) {
                    resolved_box_shadow_data.empend(
                        layer.color,
                        static_cast<int>(layer.offset_x.to_px(layout_node())),
                        static_cast<int>(layer.offset_y.to_px(layout_node())),
                        static_cast<int>(layer.blur_radius.to_px(layout_node())),
                        static_cast<int>(layer.spread_distance.to_px(layout_node())),
                        layer.placement == CSS::ShadowPlacement::Outer ? Painting::ShadowPlacement::Outer : Painting::ShadowPlacement::Inner);
                }
                Painting::paint_box_shadow(context, enclosing_int_rect(absolute_fragment_rect), resolved_box_shadow_data);
            }

            return IterationDecision::Continue;
        });
    }

    if (phase == Painting::PaintPhase::Border) {
        auto top_left_border_radius = computed_values().border_top_left_radius();
        auto top_right_border_radius = computed_values().border_top_right_radius();
        auto bottom_right_border_radius = computed_values().border_bottom_right_radius();
        auto bottom_left_border_radius = computed_values().border_bottom_left_radius();

        auto borders_data = Painting::BordersData {
            .top = computed_values().border_top(),
            .right = computed_values().border_right(),
            .bottom = computed_values().border_bottom(),
            .left = computed_values().border_left(),
        };

        auto containing_block_position_in_absolute_coordinates = containing_block()->paint_box()->absolute_position();

        for_each_fragment([&](auto const& fragment, bool is_first_fragment, bool is_last_fragment) {
            Gfx::FloatRect absolute_fragment_rect { containing_block_position_in_absolute_coordinates.translated(fragment.offset()), fragment.size() };

            if (is_first_fragment) {
                float extra_start_width = box_model().padding.left;
                absolute_fragment_rect.translate_by(-extra_start_width, 0);
                absolute_fragment_rect.set_width(absolute_fragment_rect.width() + extra_start_width);
            }

            if (is_last_fragment) {
                float extra_end_width = box_model().padding.right;
                absolute_fragment_rect.set_width(absolute_fragment_rect.width() + extra_end_width);
            }

            auto bordered_rect = absolute_fragment_rect.inflated(borders_data.top.width, borders_data.right.width, borders_data.bottom.width, borders_data.left.width);
            auto border_radius_data = Painting::normalized_border_radius_data(layout_node(), bordered_rect, top_left_border_radius, top_right_border_radius, bottom_right_border_radius, bottom_left_border_radius);

            Painting::paint_all_borders(context, bordered_rect, border_radius_data, borders_data);

            return IterationDecision::Continue;
        });
    }

    // FIXME: We check for a non-null dom_node(), since pseudo-elements have a null one and were getting
    //        highlighted incorrectly. A better solution will be needed if we want to inspect them too.
    if (phase == Painting::PaintPhase::Overlay && layout_node().dom_node() && layout_node().document().inspected_node() == layout_node().dom_node()) {
        // FIXME: This paints a double-thick border between adjacent fragments, where ideally there
        //        would be none. Once we implement non-rectangular outlines for the `outline` CSS
        //        property, we can use that here instead.
        for_each_fragment([&](auto const& fragment, bool, bool) {
            painter.draw_rect(enclosing_int_rect(fragment.absolute_rect()), Color::Magenta);
            return IterationDecision::Continue;
        });
    }
}

template<typename Callback>
void InlinePaintable::for_each_fragment(Callback callback) const
{
    // FIXME: This will be slow if the containing block has a lot of fragments!
    Vector<Layout::LineBoxFragment const&> fragments;
    containing_block()->paint_box()->for_each_fragment([&](auto& fragment) {
        if (layout_node().is_inclusive_ancestor_of(fragment.layout_node()))
            fragments.append(fragment);
        return IterationDecision::Continue;
    });
    for (size_t i = 0; i < fragments.size(); ++i) {
        auto const& fragment = fragments[i];
        callback(fragment, i == 0, i == fragments.size() - 1);
    }
}

}
