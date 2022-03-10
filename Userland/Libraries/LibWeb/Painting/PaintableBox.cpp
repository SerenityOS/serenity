/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Painting/BackgroundPainting.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Painting {

NonnullRefPtr<PaintableBox> PaintableBox::create(Layout::Box const& layout_box)
{
    return adopt_ref(*new PaintableBox(layout_box));
}

PaintableBox::PaintableBox(Layout::Box const& layout_box)
    : Paintable(layout_box)
{
}

PaintableBox::~PaintableBox()
{
}

PaintableWithLines::PaintableWithLines(Layout::BlockContainer const& layout_box)
    : PaintableBox(layout_box)
{
}

PaintableWithLines::~PaintableWithLines()
{
}

void PaintableBox::set_offset(const Gfx::FloatPoint& offset)
{
    if (m_offset == offset)
        return;
    m_offset = offset;
    // FIXME: This const_cast is gross.
    const_cast<Layout::Box&>(layout_box()).did_set_rect();
}

void PaintableBox::set_content_size(Gfx::FloatSize const& size)
{
    if (m_content_size == size)
        return;
    m_content_size = size;
    // FIXME: This const_cast is gross.
    const_cast<Layout::Box&>(layout_box()).did_set_rect();
}

Gfx::FloatPoint PaintableBox::effective_offset() const
{
    if (m_containing_line_box_fragment.has_value()) {
        auto const& fragment = layout_box().containing_block()->paint_box()->line_boxes()[m_containing_line_box_fragment->line_box_index].fragments()[m_containing_line_box_fragment->fragment_index];
        return fragment.offset();
    }
    return m_offset;
}

Gfx::FloatRect PaintableBox::absolute_rect() const
{
    Gfx::FloatRect rect { effective_offset(), content_size() };
    for (auto* block = layout_box().containing_block(); block; block = block->containing_block())
        rect.translate_by(block->paint_box()->effective_offset());
    return rect;
}

void PaintableBox::set_containing_line_box_fragment(Optional<Layout::LineBoxFragmentCoordinate> fragment_coordinate)
{
    m_containing_line_box_fragment = fragment_coordinate;
}

Painting::StackingContext* PaintableBox::enclosing_stacking_context()
{
    for (auto* ancestor = layout_box().parent(); ancestor; ancestor = ancestor->parent()) {
        if (!is<Layout::Box>(ancestor))
            continue;
        auto& ancestor_box = static_cast<Layout::Box&>(const_cast<Layout::NodeWithStyle&>(*ancestor));
        if (!ancestor_box.establishes_stacking_context())
            continue;
        VERIFY(ancestor_box.paint_box()->stacking_context());
        return const_cast<StackingContext*>(ancestor_box.paint_box()->stacking_context());
    }
    // We should always reach the Layout::InitialContainingBlock stacking context.
    VERIFY_NOT_REACHED();
}

void PaintableBox::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    if (phase == PaintPhase::Background) {
        paint_background(context);
        paint_box_shadow(context);
    }

    if (phase == PaintPhase::Border) {
        paint_border(context);
    }

    if (phase == PaintPhase::Overlay && layout_box().dom_node() && layout_box().document().inspected_node() == layout_box().dom_node()) {
        auto content_rect = absolute_rect();

        auto margin_box = box_model().margin_box();
        Gfx::FloatRect margin_rect;
        margin_rect.set_x(absolute_x() - margin_box.left);
        margin_rect.set_width(content_width() + margin_box.left + margin_box.right);
        margin_rect.set_y(absolute_y() - margin_box.top);
        margin_rect.set_height(content_height() + margin_box.top + margin_box.bottom);

        auto border_rect = absolute_border_box_rect();
        auto padding_rect = absolute_padding_box_rect();

        auto paint_inspector_rect = [&](Gfx::FloatRect const& rect, Color color) {
            context.painter().fill_rect(enclosing_int_rect(rect), Color(color).with_alpha(100));
            context.painter().draw_rect(enclosing_int_rect(rect), Color(color));
        };

        paint_inspector_rect(margin_rect, Color::Yellow);
        paint_inspector_rect(padding_rect, Color::Cyan);
        paint_inspector_rect(border_rect, Color::Green);
        paint_inspector_rect(content_rect, Color::Magenta);

        StringBuilder builder;
        if (layout_box().dom_node())
            builder.append(layout_box().dom_node()->debug_description());
        else
            builder.append(layout_box().debug_description());
        builder.appendff(" {}x{} @ {},{}", border_rect.width(), border_rect.height(), border_rect.x(), border_rect.y());
        auto size_text = builder.to_string();
        auto size_text_rect = border_rect;
        size_text_rect.set_y(border_rect.y() + border_rect.height());
        size_text_rect.set_top(size_text_rect.top());
        size_text_rect.set_width((float)context.painter().font().width(size_text) + 4);
        size_text_rect.set_height(context.painter().font().glyph_height() + 4);
        context.painter().fill_rect(enclosing_int_rect(size_text_rect), context.palette().color(Gfx::ColorRole::Tooltip));
        context.painter().draw_rect(enclosing_int_rect(size_text_rect), context.palette().threed_shadow1());
        context.painter().draw_text(enclosing_int_rect(size_text_rect), size_text, Gfx::TextAlignment::Center, context.palette().color(Gfx::ColorRole::TooltipText));
    }

    if (phase == PaintPhase::FocusOutline && layout_box().dom_node() && layout_box().dom_node()->is_element() && verify_cast<DOM::Element>(*layout_box().dom_node()).is_focused()) {
        context.painter().draw_rect(enclosing_int_rect(absolute_rect()), context.palette().focus_outline());
    }
}

void PaintableBox::paint_border(PaintContext& context) const
{
    auto borders_data = BordersData {
        .top = computed_values().border_top(),
        .right = computed_values().border_right(),
        .bottom = computed_values().border_bottom(),
        .left = computed_values().border_left(),
    };
    paint_all_borders(context, absolute_border_box_rect(), normalized_border_radius_data(), borders_data);
}

void PaintableBox::paint_background(PaintContext& context) const
{
    // If the body's background properties were propagated to the root element, do no re-paint the body's background.
    if (layout_box().is_body() && document().html_element()->should_use_body_background_properties())
        return;

    Gfx::IntRect background_rect;
    Color background_color = computed_values().background_color();
    auto* background_layers = &computed_values().background_layers();

    if (layout_box().is_root_element()) {
        // CSS 2.1 Appendix E.2: If the element is a root element, paint the background over the entire canvas.
        background_rect = context.viewport_rect();

        // Section 2.11.2: If the computed value of background-image on the root element is none and its background-color is transparent,
        // user agents must instead propagate the computed values of the background properties from that element’s first HTML BODY child element.
        if (document().html_element()->should_use_body_background_properties()) {
            background_layers = document().background_layers();
            background_color = document().background_color(context.palette());
        }
    } else {
        background_rect = enclosing_int_rect(absolute_padding_box_rect());
    }

    // HACK: If the Box has a border, use the bordered_rect to paint the background.
    //       This way if we have a border-radius there will be no gap between the filling and actual border.
    if (computed_values().border_top().width || computed_values().border_right().width || computed_values().border_bottom().width || computed_values().border_left().width)
        background_rect = enclosing_int_rect(absolute_border_box_rect());

    Painting::paint_background(context, layout_box(), background_rect, background_color, background_layers, normalized_border_radius_data());
}

void PaintableBox::paint_box_shadow(PaintContext& context) const
{
    auto box_shadow_data = computed_values().box_shadow();
    if (box_shadow_data.is_empty())
        return;

    Vector<BoxShadowData> resolved_box_shadow_data;
    resolved_box_shadow_data.ensure_capacity(box_shadow_data.size());
    for (auto const& layer : box_shadow_data) {
        resolved_box_shadow_data.empend(
            layer.color,
            static_cast<int>(layer.offset_x.to_px(layout_box())),
            static_cast<int>(layer.offset_y.to_px(layout_box())),
            static_cast<int>(layer.blur_radius.to_px(layout_box())),
            static_cast<int>(layer.spread_distance.to_px(layout_box())),
            layer.placement == CSS::BoxShadowPlacement::Outer ? BoxShadowPlacement::Outer : BoxShadowPlacement::Inner);
    }
    Painting::paint_box_shadow(context, enclosing_int_rect(absolute_border_box_rect()), resolved_box_shadow_data);
}

BorderRadiusData PaintableBox::normalized_border_radius_data() const
{
    return Painting::normalized_border_radius_data(layout_box(), absolute_border_box_rect(),
        computed_values().border_top_left_radius(),
        computed_values().border_top_right_radius(),
        computed_values().border_bottom_right_radius(),
        computed_values().border_bottom_left_radius());
}

void PaintableBox::before_children_paint(PaintContext& context, PaintPhase) const
{
    // FIXME: Support more overflow variations.
    if (computed_values().overflow_x() == CSS::Overflow::Hidden && computed_values().overflow_y() == CSS::Overflow::Hidden) {
        context.painter().save();
        context.painter().add_clip_rect(enclosing_int_rect(absolute_border_box_rect()));
    }
}

void PaintableBox::after_children_paint(PaintContext& context, PaintPhase) const
{
    // FIXME: Support more overflow variations.
    if (computed_values().overflow_x() == CSS::Overflow::Hidden && computed_values().overflow_y() == CSS::Overflow::Hidden)
        context.painter().restore();
}

void PaintableWithLines::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    PaintableBox::paint(context, phase);

    if (m_line_boxes.is_empty())
        return;

    bool should_clip_overflow = computed_values().overflow_x() != CSS::Overflow::Visible && computed_values().overflow_y() != CSS::Overflow::Visible;

    if (should_clip_overflow) {
        context.painter().save();
        // FIXME: Handle overflow-x and overflow-y being different values.
        context.painter().add_clip_rect(enclosing_int_rect(absolute_padding_box_rect()));
        auto scroll_offset = static_cast<Layout::BlockContainer const&>(layout_box()).scroll_offset();
        context.painter().translate(-scroll_offset.to_type<int>());
    }

    for (auto& line_box : m_line_boxes) {
        for (auto& fragment : line_box.fragments()) {
            if (context.should_show_line_box_borders())
                context.painter().draw_rect(enclosing_int_rect(fragment.absolute_rect()), Color::Green);
            const_cast<Layout::LineBoxFragment&>(fragment).paint(context, phase);
        }
    }

    if (should_clip_overflow) {
        context.painter().restore();
    }

    // FIXME: Merge this loop with the above somehow..
    if (phase == PaintPhase::FocusOutline) {
        for (auto& line_box : m_line_boxes) {
            for (auto& fragment : line_box.fragments()) {
                auto* node = fragment.layout_node().dom_node();
                if (!node)
                    continue;
                auto* parent = node->parent_element();
                if (!parent)
                    continue;
                if (parent->is_focused())
                    context.painter().draw_rect(enclosing_int_rect(fragment.absolute_rect()), context.palette().focus_outline());
            }
        }
    }
}

bool PaintableWithLines::handle_mousewheel(Badge<EventHandler>, Gfx::IntPoint const&, unsigned, unsigned, int wheel_delta_x, int wheel_delta_y)
{
    if (!layout_box().is_scrollable())
        return false;
    auto new_offset = layout_box().scroll_offset();
    new_offset.translate_by(wheel_delta_x, wheel_delta_y);
    const_cast<Layout::BlockContainer&>(layout_box()).set_scroll_offset(new_offset);
    return true;
}

Layout::BlockContainer const& PaintableWithLines::layout_box() const
{
    return static_cast<Layout::BlockContainer const&>(PaintableBox::layout_box());
}

Layout::BlockContainer& PaintableWithLines::layout_box()
{
    return static_cast<Layout::BlockContainer&>(PaintableBox::layout_box());
}

}
