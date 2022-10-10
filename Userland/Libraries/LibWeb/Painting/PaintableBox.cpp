/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericShorthands.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Painting/BackgroundPainting.h>
#include <LibWeb/Painting/FilterPainting.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/Platform/FontPlugin.h>

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

void PaintableBox::invalidate_stacking_context()
{
    m_stacking_context = nullptr;
}

bool PaintableBox::is_out_of_view(PaintContext& context) const
{
    return !enclosing_int_rect(absolute_paint_rect())
                .translated(context.painter().translation())
                .intersects(context.painter().clip_rect());
}

PaintableWithLines::PaintableWithLines(Layout::BlockContainer const& layout_box)
    : PaintableBox(layout_box)
{
}

PaintableWithLines::~PaintableWithLines()
{
}

void PaintableBox::set_offset(Gfx::FloatPoint const& offset)
{
    m_offset = offset;
    // FIXME: This const_cast is gross.
    const_cast<Layout::Box&>(layout_box()).did_set_rect();
}

void PaintableBox::set_content_size(Gfx::FloatSize const& size)
{
    m_content_size = size;
    // FIXME: This const_cast is gross.
    const_cast<Layout::Box&>(layout_box()).did_set_rect();
}

Gfx::FloatPoint PaintableBox::effective_offset() const
{
    Gfx::FloatPoint offset;
    if (m_containing_line_box_fragment.has_value()) {
        auto const& fragment = containing_block()->paint_box()->line_boxes()[m_containing_line_box_fragment->line_box_index].fragments()[m_containing_line_box_fragment->fragment_index];
        offset = fragment.offset();
    } else {
        offset = m_offset;
    }
    if (layout_box().computed_values().position() == CSS::Position::Relative) {
        auto const& inset = layout_box().box_model().inset;
        offset.translate_by(inset.left, inset.top);
    }
    return offset;
}

Gfx::FloatRect PaintableBox::compute_absolute_rect() const
{
    Gfx::FloatRect rect { effective_offset(), content_size() };
    for (auto const* block = containing_block(); block && block->paintable(); block = block->paintable()->containing_block())
        rect.translate_by(block->paint_box()->effective_offset());
    return rect;
}

Gfx::FloatRect PaintableBox::absolute_rect() const
{
    if (!m_absolute_rect.has_value())
        m_absolute_rect = compute_absolute_rect();
    return *m_absolute_rect;
}

Gfx::FloatRect PaintableBox::compute_absolute_paint_rect() const
{
    // FIXME: This likely incomplete:
    auto rect = absolute_border_box_rect();
    auto resolved_box_shadow_data = resolve_box_shadow_data();
    for (auto const& shadow : resolved_box_shadow_data) {
        if (shadow.placement == ShadowPlacement::Inner)
            continue;
        auto inflate = shadow.spread_distance + shadow.blur_radius;
        auto shadow_rect = rect.inflated(inflate, inflate, inflate, inflate).translated(shadow.offset_x, shadow.offset_y);
        rect = rect.united(shadow_rect);
    }
    return rect;
}

Gfx::FloatRect PaintableBox::absolute_paint_rect() const
{
    if (!m_absolute_paint_rect.has_value())
        m_absolute_paint_rect = compute_absolute_paint_rect();
    return *m_absolute_paint_rect;
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
        if (auto* ancestor_paint_box = ancestor_box.paint_box(); ancestor_paint_box && ancestor_paint_box->stacking_context())
            return const_cast<StackingContext*>(ancestor_paint_box->stacking_context());
    }
    // We should always reach the Layout::InitialContainingBlock stacking context.
    VERIFY_NOT_REACHED();
}

void PaintableBox::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    auto clip_rect = computed_values().clip();
    auto should_clip_rect = clip_rect.is_rect() && layout_box().is_absolutely_positioned();

    if (phase == PaintPhase::Background) {
        if (should_clip_rect) {
            context.painter().save();
            auto border_box = absolute_border_box_rect();
            context.painter().add_clip_rect(clip_rect.to_rect().resolved(Paintable::layout_node(), border_box).to_rounded<int>());
        }
        paint_backdrop_filter(context);
        paint_background(context);
        paint_box_shadow(context);
    }

    if (phase == PaintPhase::Border) {
        paint_border(context);
    }

    if (phase == PaintPhase::Overlay && should_clip_rect)
        context.painter().restore();

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

        auto& font = Platform::FontPlugin::the().default_font();

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
        size_text_rect.set_width((float)font.width(size_text) + 4);
        size_text_rect.set_height(font.pixel_size() + 4);
        context.painter().fill_rect(enclosing_int_rect(size_text_rect), context.palette().color(Gfx::ColorRole::Tooltip));
        context.painter().draw_rect(enclosing_int_rect(size_text_rect), context.palette().threed_shadow1());
        context.painter().draw_text(enclosing_int_rect(size_text_rect), size_text, font, Gfx::TextAlignment::Center, context.palette().color(Gfx::ColorRole::TooltipText));
    }

    if (phase == PaintPhase::FocusOutline && layout_box().dom_node() && layout_box().dom_node()->is_element() && verify_cast<DOM::Element>(*layout_box().dom_node()).is_focused()) {
        // FIXME: Implement this as `outline` using :focus-visible in the default UA stylesheet to make it possible to override/disable.
        auto focus_outline_rect = enclosing_int_rect(absolute_border_box_rect()).inflated(4, 4);
        context.painter().draw_focus_rect(focus_outline_rect, context.palette().focus_outline());
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
    paint_all_borders(context, absolute_border_box_rect(), normalized_border_radii_data(), borders_data);
}

void PaintableBox::paint_backdrop_filter(PaintContext& context) const
{
    auto& backdrop_filter = computed_values().backdrop_filter();
    if (!backdrop_filter.is_none())
        Painting::apply_backdrop_filter(context, layout_node(), absolute_border_box_rect(), normalized_border_radii_data(), backdrop_filter);
}

void PaintableBox::paint_background(PaintContext& context) const
{
    // If the body's background properties were propagated to the root element, do no re-paint the body's background.
    if (layout_box().is_body() && document().html_element()->should_use_body_background_properties())
        return;

    Gfx::FloatRect background_rect;
    Color background_color = computed_values().background_color();
    auto* background_layers = &computed_values().background_layers();

    if (layout_box().is_root_element()) {
        // CSS 2.1 Appendix E.2: If the element is a root element, paint the background over the entire canvas.
        background_rect = context.viewport_rect().to_type<float>();

        // Section 2.11.2: If the computed value of background-image on the root element is none and its background-color is transparent,
        // user agents must instead propagate the computed values of the background properties from that element’s first HTML BODY child element.
        if (document().html_element()->should_use_body_background_properties()) {
            background_layers = document().background_layers();
            background_color = document().background_color(context.palette());
        }
    } else {
        background_rect = absolute_padding_box_rect();
    }

    // HACK: If the Box has a border, use the bordered_rect to paint the background.
    //       This way if we have a border-radius there will be no gap between the filling and actual border.
    if (computed_values().border_top().width || computed_values().border_right().width || computed_values().border_bottom().width || computed_values().border_left().width)
        background_rect = absolute_border_box_rect();

    Painting::paint_background(context, layout_box(), background_rect, background_color, computed_values().image_rendering(), background_layers, normalized_border_radii_data());
}

Vector<ShadowData> PaintableBox::resolve_box_shadow_data() const
{
    auto box_shadow_data = computed_values().box_shadow();
    if (box_shadow_data.is_empty())
        return {};

    Vector<ShadowData> resolved_box_shadow_data;
    resolved_box_shadow_data.ensure_capacity(box_shadow_data.size());
    for (auto const& layer : box_shadow_data) {
        resolved_box_shadow_data.empend(
            layer.color,
            static_cast<int>(layer.offset_x.to_px(layout_box())),
            static_cast<int>(layer.offset_y.to_px(layout_box())),
            static_cast<int>(layer.blur_radius.to_px(layout_box())),
            static_cast<int>(layer.spread_distance.to_px(layout_box())),
            layer.placement == CSS::ShadowPlacement::Outer ? ShadowPlacement::Outer : ShadowPlacement::Inner);
    }

    return resolved_box_shadow_data;
}

void PaintableBox::paint_box_shadow(PaintContext& context) const
{
    auto resolved_box_shadow_data = resolve_box_shadow_data();
    if (resolved_box_shadow_data.is_empty())
        return;
    Painting::paint_box_shadow(context, absolute_border_box_rect().to_rounded<int>(), normalized_border_radii_data(), resolved_box_shadow_data);
}

BorderRadiiData PaintableBox::normalized_border_radii_data(ShrinkRadiiForBorders shrink) const
{
    auto border_radius_data = Painting::normalized_border_radii_data(layout_box(), absolute_border_box_rect(),
        computed_values().border_top_left_radius(),
        computed_values().border_top_right_radius(),
        computed_values().border_bottom_right_radius(),
        computed_values().border_bottom_left_radius());
    if (shrink == ShrinkRadiiForBorders::Yes)
        border_radius_data.shrink(computed_values().border_top().width, computed_values().border_right().width, computed_values().border_bottom().width, computed_values().border_left().width);
    return border_radius_data;
}

void PaintableBox::before_children_paint(PaintContext& context, PaintPhase phase, ShouldClipOverflow should_clip_overflow) const
{
    if (!AK::first_is_one_of(phase, PaintPhase::Background, PaintPhase::Border, PaintPhase::Foreground))
        return;

    if (should_clip_overflow == ShouldClipOverflow::No)
        return;

    // FIXME: Support more overflow variations.
    auto clip_rect = absolute_padding_box_rect().to_rounded<int>();
    auto overflow_x = computed_values().overflow_x();
    auto overflow_y = computed_values().overflow_y();

    auto clip_overflow = [&] {
        if (!m_clipping_overflow) {
            context.painter().save();
            context.painter().add_clip_rect(clip_rect);
            m_clipping_overflow = true;
        }
    };

    if (overflow_x == CSS::Overflow::Hidden && overflow_y == CSS::Overflow::Hidden) {
        clip_overflow();
    }
    if (overflow_y == CSS::Overflow::Hidden || overflow_x == CSS::Overflow::Hidden) {
        auto border_radii_data = normalized_border_radii_data(ShrinkRadiiForBorders::Yes);
        if (border_radii_data.has_any_radius()) {
            auto corner_clipper = BorderRadiusCornerClipper::create(clip_rect, border_radii_data, CornerClip::Outside, BorderRadiusCornerClipper::UseCachedBitmap::No);
            if (corner_clipper.is_error()) {
                dbgln("Failed to create overflow border-radius corner clipper: {}", corner_clipper.error());
                return;
            }
            clip_overflow();
            m_overflow_corner_radius_clipper = corner_clipper.release_value();
            m_overflow_corner_radius_clipper->sample_under_corners(context.painter());
        }
    }
}

void PaintableBox::after_children_paint(PaintContext& context, PaintPhase phase, ShouldClipOverflow should_clip_overflow) const
{
    if (!AK::first_is_one_of(phase, PaintPhase::Background, PaintPhase::Border, PaintPhase::Foreground))
        return;

    if (should_clip_overflow == ShouldClipOverflow::No)
        return;

    // FIXME: Support more overflow variations.
    if (m_clipping_overflow) {
        context.painter().restore();
        m_clipping_overflow = false;
    }
    if (m_overflow_corner_radius_clipper.has_value()) {
        m_overflow_corner_radius_clipper->blit_corner_clipping(context.painter());
        m_overflow_corner_radius_clipper = {};
    }
}

static void paint_cursor_if_needed(PaintContext& context, Layout::TextNode const& text_node, Layout::LineBoxFragment const& fragment)
{
    auto const& browsing_context = text_node.browsing_context();

    if (!browsing_context.is_focused_context())
        return;

    if (!browsing_context.cursor_blink_state())
        return;

    if (browsing_context.cursor_position().node() != &text_node.dom_node())
        return;

    // NOTE: This checks if the cursor is before the start or after the end of the fragment. If it is at the end, after all text, it should still be painted.
    if (browsing_context.cursor_position().offset() < (unsigned)fragment.start() || browsing_context.cursor_position().offset() > (unsigned)(fragment.start() + fragment.length()))
        return;

    if (!fragment.layout_node().dom_node() || !fragment.layout_node().dom_node()->is_editable())
        return;

    auto fragment_rect = fragment.absolute_rect();

    float cursor_x = fragment_rect.x() + text_node.font().width(fragment.text().substring_view(0, text_node.browsing_context().cursor_position().offset() - fragment.start()));
    float cursor_top = fragment_rect.top();
    float cursor_height = fragment_rect.height();
    Gfx::IntRect cursor_rect(cursor_x, cursor_top, 1, cursor_height);

    context.painter().draw_rect(cursor_rect, text_node.computed_values().color());
}

static void paint_text_decoration(Gfx::Painter& painter, Layout::Node const& text_node, Layout::LineBoxFragment const& fragment)
{
    auto& font = fragment.layout_node().font();
    auto fragment_box = enclosing_int_rect(fragment.absolute_rect());
    auto glyph_height = font.pixel_size();
    auto baseline = fragment_box.height() / 2 - (glyph_height + 4) / 2 + glyph_height;

    auto line_color = text_node.computed_values().text_decoration_color();

    int line_thickness = [&] {
        CSS::Length computed_thickness = text_node.computed_values().text_decoration_thickness().resolved(text_node, CSS::Length(1, CSS::Length::Type::Em));
        if (computed_thickness.is_auto())
            return max(glyph_height * 0.1f, 1.f);

        return computed_thickness.to_px(text_node);
    }();

    auto text_decoration_lines = text_node.computed_values().text_decoration_line();
    for (auto line : text_decoration_lines) {
        Gfx::IntPoint line_start_point {};
        Gfx::IntPoint line_end_point {};

        switch (line) {
        case CSS::TextDecorationLine::None:
            return;
        case CSS::TextDecorationLine::Underline:
            line_start_point = fragment_box.top_left().translated(0, baseline + 2);
            line_end_point = fragment_box.top_right().translated(0, baseline + 2);
            break;
        case CSS::TextDecorationLine::Overline:
            line_start_point = fragment_box.top_left().translated(0, baseline - glyph_height);
            line_end_point = fragment_box.top_right().translated(0, baseline - glyph_height);
            break;
        case CSS::TextDecorationLine::LineThrough: {
            auto x_height = font.x_height();
            line_start_point = fragment_box.top_left().translated(0, baseline - x_height / 2);
            line_end_point = fragment_box.top_right().translated(0, baseline - x_height / 2);
            break;
        }
        case CSS::TextDecorationLine::Blink:
            // Conforming user agents may simply not blink the text
            return;
        }

        switch (text_node.computed_values().text_decoration_style()) {
        case CSS::TextDecorationStyle::Solid:
            painter.draw_line(line_start_point, line_end_point, line_color, line_thickness, Gfx::Painter::LineStyle::Solid);
            break;
        case CSS::TextDecorationStyle::Double:
            switch (line) {
            case CSS::TextDecorationLine::Underline:
                break;
            case CSS::TextDecorationLine::Overline:
                line_start_point.translate_by(0, -line_thickness - 1);
                line_end_point.translate_by(0, -line_thickness - 1);
                break;
            case CSS::TextDecorationLine::LineThrough:
                line_start_point.translate_by(0, -line_thickness / 2);
                line_end_point.translate_by(0, -line_thickness / 2);
                break;
            default:
                VERIFY_NOT_REACHED();
            }

            painter.draw_line(line_start_point, line_end_point, line_color, line_thickness);
            painter.draw_line(line_start_point.translated(0, line_thickness + 1), line_end_point.translated(0, line_thickness + 1), line_color, line_thickness);
            break;
        case CSS::TextDecorationStyle::Dashed:
            painter.draw_line(line_start_point, line_end_point, line_color, line_thickness, Gfx::Painter::LineStyle::Dashed);
            break;
        case CSS::TextDecorationStyle::Dotted:
            painter.draw_line(line_start_point, line_end_point, line_color, line_thickness, Gfx::Painter::LineStyle::Dotted);
            break;
        case CSS::TextDecorationStyle::Wavy:
            painter.draw_triangle_wave(line_start_point, line_end_point, line_color, line_thickness + 1, line_thickness);
            break;
        }
    }
}

static void paint_text_fragment(PaintContext& context, Layout::TextNode const& text_node, Layout::LineBoxFragment const& fragment, Painting::PaintPhase phase)
{
    auto& painter = context.painter();

    if (phase == Painting::PaintPhase::Foreground) {
        auto fragment_absolute_rect = fragment.absolute_rect();

        if (text_node.document().inspected_node() == &text_node.dom_node())
            context.painter().draw_rect(enclosing_int_rect(fragment_absolute_rect), Color::Magenta);

        // FIXME: text-transform should be done already in layout, since uppercase glyphs may be wider than lowercase, etc.
        auto text = text_node.text_for_rendering();
        auto text_transform = text_node.computed_values().text_transform();
        if (text_transform == CSS::TextTransform::Uppercase)
            text = Unicode::to_unicode_uppercase_full(text_node.text_for_rendering());
        if (text_transform == CSS::TextTransform::Lowercase)
            text = Unicode::to_unicode_lowercase_full(text_node.text_for_rendering());

        Gfx::FloatPoint baseline_start { fragment_absolute_rect.x(), fragment_absolute_rect.y() + fragment.baseline() };
        Utf8View view { text.substring_view(fragment.start(), fragment.length()) };

        painter.draw_text_run(baseline_start, view, fragment.layout_node().font(), text_node.computed_values().color());

        auto selection_rect = fragment.selection_rect(text_node.font());
        if (!selection_rect.is_empty()) {
            painter.fill_rect(enclosing_int_rect(selection_rect), context.palette().selection());
            Gfx::PainterStateSaver saver(painter);
            painter.add_clip_rect(enclosing_int_rect(selection_rect));
            painter.draw_text_run(baseline_start, view, fragment.layout_node().font(), context.palette().selection_text());
        }

        paint_text_decoration(painter, text_node, fragment);
        paint_cursor_if_needed(context, text_node, fragment);
    }
}

void PaintableWithLines::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    PaintableBox::paint(context, phase);

    if (m_line_boxes.is_empty())
        return;

    bool should_clip_overflow = computed_values().overflow_x() != CSS::Overflow::Visible && computed_values().overflow_y() != CSS::Overflow::Visible;
    Optional<BorderRadiusCornerClipper> corner_clipper;

    if (should_clip_overflow) {
        context.painter().save();
        // FIXME: Handle overflow-x and overflow-y being different values.
        auto clip_box = absolute_padding_box_rect().to_rounded<int>();
        context.painter().add_clip_rect(clip_box);
        auto scroll_offset = static_cast<Layout::BlockContainer const&>(layout_box()).scroll_offset();
        context.painter().translate(-scroll_offset.to_type<int>());

        auto border_radii = normalized_border_radii_data(ShrinkRadiiForBorders::Yes);
        if (border_radii.has_any_radius()) {
            auto clipper = BorderRadiusCornerClipper::create(clip_box, border_radii);
            if (!clipper.is_error()) {
                corner_clipper = clipper.release_value();
                corner_clipper->sample_under_corners(context.painter());
            }
        }
    }

    // Text shadows
    // This is yet another loop, but done here because all shadows should appear under all text.
    // So, we paint the shadows before painting any text.
    // FIXME: Find a smarter way to do this?
    if (phase == PaintPhase::Foreground) {
        for (auto& line_box : m_line_boxes) {
            for (auto& fragment : line_box.fragments()) {
                if (is<Layout::TextNode>(fragment.layout_node())) {
                    auto& text_shadow = fragment.layout_node().computed_values().text_shadow();
                    if (!text_shadow.is_empty()) {
                        Vector<ShadowData> resolved_shadow_data;
                        resolved_shadow_data.ensure_capacity(text_shadow.size());
                        for (auto const& layer : text_shadow) {
                            resolved_shadow_data.empend(
                                layer.color,
                                static_cast<int>(layer.offset_x.to_px(layout_box())),
                                static_cast<int>(layer.offset_y.to_px(layout_box())),
                                static_cast<int>(layer.blur_radius.to_px(layout_box())),
                                static_cast<int>(layer.spread_distance.to_px(layout_box())),
                                ShadowPlacement::Outer);
                        }
                        context.painter().set_font(fragment.layout_node().font());
                        Painting::paint_text_shadow(context, fragment, resolved_shadow_data);
                    }
                }
            }
        }
    }

    for (auto& line_box : m_line_boxes) {
        for (auto& fragment : line_box.fragments()) {
            if (context.should_show_line_box_borders()) {
                auto fragment_absolute_rect = fragment.absolute_rect();
                context.painter().draw_rect(enclosing_int_rect(fragment_absolute_rect), Color::Green);
                context.painter().draw_line(
                    fragment_absolute_rect.top_left().translated(0, fragment.baseline()).to_rounded<int>(),
                    fragment_absolute_rect.top_right().translated(0, fragment.baseline()).to_rounded<int>(), Color::Red);
            }
            if (is<Layout::TextNode>(fragment.layout_node()))
                paint_text_fragment(context, static_cast<Layout::TextNode const&>(fragment.layout_node()), fragment, phase);
        }
    }

    if (should_clip_overflow) {
        context.painter().restore();
        if (corner_clipper.has_value())
            corner_clipper->blit_corner_clipping(context.painter());
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
                if (parent->is_focused()) {
                    // FIXME: Implement this as `outline` using :focus-visible in the default UA stylesheet to make it possible to override/disable.
                    auto focus_outline_rect = enclosing_int_rect(fragment.absolute_rect()).inflated(4, 4);
                    context.painter().draw_focus_rect(focus_outline_rect, context.palette().focus_outline());
                }
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

void PaintableBox::set_stacking_context(NonnullOwnPtr<StackingContext> stacking_context)
{
    m_stacking_context = move(stacking_context);
}

Optional<HitTestResult> PaintableBox::hit_test(Gfx::FloatPoint const& position, HitTestType type) const
{
    if (!is_visible())
        return {};

    if (layout_box().is_initial_containing_block_box()) {
        const_cast<Layout::InitialContainingBlock&>(static_cast<Layout::InitialContainingBlock const&>(layout_box())).build_stacking_context_tree_if_needed();
        return stacking_context()->hit_test(position, type);
    }

    if (absolute_border_box_rect().contains(position.x(), position.y()))
        return HitTestResult { *this };
    return {};
}

Optional<HitTestResult> PaintableWithLines::hit_test(Gfx::FloatPoint const& position, HitTestType type) const
{
    if (!layout_box().children_are_inline())
        return PaintableBox::hit_test(position, type);

    Optional<HitTestResult> last_good_candidate;
    for (auto& line_box : m_line_boxes) {
        for (auto& fragment : line_box.fragments()) {
            if (is<Layout::Box>(fragment.layout_node()) && static_cast<Layout::Box const&>(fragment.layout_node()).paint_box()->stacking_context())
                continue;
            auto fragment_absolute_rect = fragment.absolute_rect();
            if (fragment_absolute_rect.contains(position)) {
                if (is<Layout::BlockContainer>(fragment.layout_node()) && fragment.layout_node().paintable())
                    return fragment.layout_node().paintable()->hit_test(position, type);
                return HitTestResult { *fragment.layout_node().paintable(), fragment.text_index_at(position.x()) };
            }
            if (fragment_absolute_rect.top() <= position.y())
                last_good_candidate = HitTestResult { *fragment.layout_node().paintable(), fragment.text_index_at(position.x()) };
        }
    }

    if (type == HitTestType::TextCursor && last_good_candidate.has_value())
        return last_good_candidate;
    if (is_visible() && absolute_border_box_rect().contains(position.x(), position.y()))
        return HitTestResult { *this };
    return {};
}

}
