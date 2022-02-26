/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Painting/BackgroundPainting.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Layout {

void Box::paint(PaintContext& context, PaintPhase phase)
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

    if (phase == PaintPhase::Overlay && dom_node() && document().inspected_node() == dom_node()) {
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
        if (dom_node())
            builder.append(dom_node()->debug_description());
        else
            builder.append(debug_description());
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

    if (phase == PaintPhase::FocusOutline && dom_node() && dom_node()->is_element() && verify_cast<DOM::Element>(*dom_node()).is_focused()) {
        context.painter().draw_rect(enclosing_int_rect(absolute_rect()), context.palette().focus_outline());
    }
}

void Box::paint_border(PaintContext& context)
{
    auto borders_data = Painting::BordersData {
        .top = computed_values().border_top(),
        .right = computed_values().border_right(),
        .bottom = computed_values().border_bottom(),
        .left = computed_values().border_left(),
    };
    Painting::paint_all_borders(context, absolute_border_box_rect(), normalized_border_radius_data(), borders_data);
}

void Box::paint_background(PaintContext& context)
{
    // If the body's background properties were propagated to the root element, do no re-paint the body's background.
    if (is_body() && document().html_element()->should_use_body_background_properties())
        return;

    Gfx::IntRect background_rect;
    Color background_color = computed_values().background_color();
    auto* background_layers = &computed_values().background_layers();

    if (is_root_element()) {
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

    Painting::paint_background(context, *this, background_rect, background_color, background_layers, normalized_border_radius_data());
}

void Box::paint_box_shadow(PaintContext& context)
{
    auto box_shadow_data = computed_values().box_shadow();
    if (box_shadow_data.is_empty())
        return;

    Vector<Painting::BoxShadowData> resolved_box_shadow_data;
    resolved_box_shadow_data.ensure_capacity(box_shadow_data.size());
    for (auto const& layer : box_shadow_data) {
        resolved_box_shadow_data.empend(
            layer.color,
            static_cast<int>(layer.offset_x.to_px(*this)),
            static_cast<int>(layer.offset_y.to_px(*this)),
            static_cast<int>(layer.blur_radius.to_px(*this)),
            static_cast<int>(layer.spread_distance.to_px(*this)),
            layer.placement == CSS::BoxShadowPlacement::Outer ? Painting::BoxShadowPlacement::Outer : Painting::BoxShadowPlacement::Inner);
    }
    Painting::paint_box_shadow(context, enclosing_int_rect(absolute_border_box_rect()), resolved_box_shadow_data);
}

Painting::BorderRadiusData Box::normalized_border_radius_data()
{
    return Painting::normalized_border_radius_data(*this, absolute_border_box_rect(),
        computed_values().border_top_left_radius(),
        computed_values().border_top_right_radius(),
        computed_values().border_bottom_right_radius(),
        computed_values().border_bottom_left_radius());
}

// https://www.w3.org/TR/css-display-3/#out-of-flow
bool Box::is_out_of_flow(FormattingContext const& formatting_context) const
{
    // A box is out of flow if either:

    // 1. It is floated (which requires that floating is not inhibited).
    if (!formatting_context.inhibits_floating() && computed_values().float_() != CSS::Float::None)
        return true;

    // 2. It is "absolutely positioned".
    switch (computed_values().position()) {
    case CSS::Position::Absolute:
    case CSS::Position::Fixed:
        return true;
    case CSS::Position::Static:
    case CSS::Position::Relative:
    case CSS::Position::Sticky:
        break;
    }

    return false;
}

HitTestResult Box::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    // FIXME: It would be nice if we could confidently skip over hit testing
    //        parts of the layout tree, but currently we can't just check
    //        m_rect.contains() since inline text rects can't be trusted..
    HitTestResult result { absolute_rect().contains(position.x(), position.y()) ? this : nullptr };
    for_each_child_in_paint_order([&](auto& child) {
        auto child_result = child.hit_test(position, type);
        if (child_result.layout_node)
            result = child_result;
    });
    return result;
}

void Box::set_needs_display()
{
    if (!is_inline()) {
        browsing_context().set_needs_display(enclosing_int_rect(absolute_rect()));
        return;
    }

    Node::set_needs_display();
}

bool Box::is_body() const
{
    return dom_node() && dom_node() == document().body();
}

void Box::set_offset(const Gfx::FloatPoint& offset)
{
    if (m_offset == offset)
        return;
    m_offset = offset;
    did_set_rect();
}

void Box::set_content_size(Gfx::FloatSize const& size)
{
    if (m_content_size == size)
        return;
    m_content_size = size;
    did_set_rect();
}

Gfx::FloatPoint Box::effective_offset() const
{
    if (m_containing_line_box_fragment)
        return m_containing_line_box_fragment->offset();
    return m_offset;
}

const Gfx::FloatRect Box::absolute_rect() const
{
    Gfx::FloatRect rect { effective_offset(), content_size() };
    for (auto* block = containing_block(); block; block = block->containing_block()) {
        rect.translate_by(block->effective_offset());
    }
    return rect;
}

void Box::set_containing_line_box_fragment(LineBoxFragment& fragment)
{
    m_containing_line_box_fragment = fragment.make_weak_ptr();
}

StackingContext* Box::enclosing_stacking_context()
{
    for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (!is<Box>(ancestor))
            continue;
        auto& ancestor_box = verify_cast<Box>(*ancestor);
        if (!ancestor_box.establishes_stacking_context())
            continue;
        VERIFY(ancestor_box.stacking_context());
        return ancestor_box.stacking_context();
    }
    // We should always reach the Layout::InitialContainingBlock stacking context.
    VERIFY_NOT_REACHED();
}

void Box::before_children_paint(PaintContext& context, PaintPhase phase)
{
    NodeWithStyleAndBoxModelMetrics::before_children_paint(context, phase);
    // FIXME: Support more overflow variations.
    if (computed_values().overflow_x() == CSS::Overflow::Hidden && computed_values().overflow_y() == CSS::Overflow::Hidden) {
        context.painter().save();
        context.painter().add_clip_rect(enclosing_int_rect(absolute_border_box_rect()));
    }
}

void Box::after_children_paint(PaintContext& context, PaintPhase phase)
{
    NodeWithStyleAndBoxModelMetrics::after_children_paint(context, phase);
    // FIXME: Support more overflow variations.
    if (computed_values().overflow_x() == CSS::Overflow::Hidden && computed_values().overflow_y() == CSS::Overflow::Hidden)
        context.painter().restore();
}

}
