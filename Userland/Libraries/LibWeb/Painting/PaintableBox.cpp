/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericShorthands.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibWeb/CSS/SystemColor.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/BackgroundPainting.h>
#include <LibWeb/Painting/FilterPainting.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/Painting/TextPaintable.h>
#include <LibWeb/Painting/ViewportPaintable.h>
#include <LibWeb/Platform/FontPlugin.h>

namespace Web::Painting {

JS::NonnullGCPtr<PaintableWithLines> PaintableWithLines::create(Layout::BlockContainer const& block_container)
{
    return block_container.heap().allocate_without_realm<PaintableWithLines>(block_container);
}

JS::NonnullGCPtr<PaintableBox> PaintableBox::create(Layout::Box const& layout_box)
{
    return layout_box.heap().allocate_without_realm<PaintableBox>(layout_box);
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

CSSPixelPoint PaintableBox::scroll_offset() const
{
    auto const& node = layout_node();
    if (node.is_generated_for_before_pseudo_element())
        return node.pseudo_element_generator()->scroll_offset(DOM::Element::ScrollOffsetFor::PseudoBefore);
    if (node.is_generated_for_after_pseudo_element())
        return node.pseudo_element_generator()->scroll_offset(DOM::Element::ScrollOffsetFor::PseudoAfter);

    if (!(dom_node() && is<DOM::Element>(*dom_node())))
        return {};

    return static_cast<DOM::Element const*>(dom_node())->scroll_offset(DOM::Element::ScrollOffsetFor::Self);
}

void PaintableBox::set_scroll_offset(CSSPixelPoint offset)
{
    // FIXME: If there is horizontal and vertical scroll ignore only part of the new offset
    if (offset.y() < 0 || scroll_offset() == offset)
        return;

    auto& node = layout_node();
    if (node.is_generated_for_before_pseudo_element()) {
        node.pseudo_element_generator()->set_scroll_offset(DOM::Element::ScrollOffsetFor::PseudoBefore, offset);
    } else if (node.is_generated_for_after_pseudo_element()) {
        node.pseudo_element_generator()->set_scroll_offset(DOM::Element::ScrollOffsetFor::PseudoAfter, offset);
    } else if (is<DOM::Element>(*dom_node())) {
        static_cast<DOM::Element*>(dom_node())->set_scroll_offset(DOM::Element::ScrollOffsetFor::Self, offset);
    } else {
        return;
    }

    set_needs_display();
}

void PaintableBox::scroll_by(int delta_x, int delta_y)
{
    auto scrollable_overflow_rect = this->scrollable_overflow_rect();
    if (!scrollable_overflow_rect.has_value())
        return;
    auto max_x_offset = scrollable_overflow_rect->width() - content_size().width();
    auto max_y_offset = scrollable_overflow_rect->height() - content_size().height();
    auto current_offset = scroll_offset();
    auto new_offset_x = clamp(current_offset.x() + delta_x, 0, max_x_offset);
    auto new_offset_y = clamp(current_offset.y() + delta_y, 0, max_y_offset);
    set_scroll_offset({ new_offset_x, new_offset_y });
}

void PaintableBox::set_offset(CSSPixelPoint offset)
{
    m_offset = offset;
}

void PaintableBox::set_content_size(CSSPixelSize size)
{
    m_content_size = size;
    layout_box().did_set_content_size();
}

CSSPixelPoint PaintableBox::offset() const
{
    return m_offset;
}

CSSPixelRect PaintableBox::compute_absolute_rect() const
{
    CSSPixelRect rect { offset(), content_size() };
    for (auto const* block = containing_block(); block && block->paintable(); block = block->paintable()->containing_block())
        rect.translate_by(block->paintable_box()->offset());
    return rect;
}

CSSPixelRect PaintableBox::compute_absolute_padding_rect_with_css_transform_applied() const
{
    CSSPixelRect rect { offset(), content_size() };
    for (auto const* block = containing_block(); block; block = block->containing_block()) {
        auto offset = block->paintable_box()->offset();
        auto affine_transform = Gfx::extract_2d_affine_transform(block->paintable_box()->transform());
        offset.translate_by(affine_transform.translation().to_type<CSSPixels>());
        rect.translate_by(offset);
    }
    auto affine_transform = Gfx::extract_2d_affine_transform(transform());
    rect.translate_by(affine_transform.translation().to_type<CSSPixels>());

    CSSPixelRect padding_rect;
    padding_rect.set_x(rect.x() - box_model().padding.left);
    padding_rect.set_width(content_width() + box_model().padding.left + box_model().padding.right);
    padding_rect.set_y(rect.y() - box_model().padding.top);
    padding_rect.set_height(content_height() + box_model().padding.top + box_model().padding.bottom);
    return padding_rect;
}

CSSPixelRect PaintableBox::absolute_rect() const
{
    if (!m_absolute_rect.has_value())
        m_absolute_rect = compute_absolute_rect();
    return *m_absolute_rect;
}

CSSPixelRect PaintableBox::compute_absolute_paint_rect() const
{
    // FIXME: This likely incomplete:
    auto rect = absolute_border_box_rect();
    if (has_scrollable_overflow()) {
        auto scrollable_overflow_rect = this->scrollable_overflow_rect().value();
        if (computed_values().overflow_x() == CSS::Overflow::Visible)
            rect.unite_horizontally(scrollable_overflow_rect);
        if (computed_values().overflow_y() == CSS::Overflow::Visible)
            rect.unite_vertically(scrollable_overflow_rect);
    }
    for (auto const& shadow : box_shadow_data()) {
        if (shadow.placement == ShadowPlacement::Inner)
            continue;
        auto inflate = shadow.spread_distance + shadow.blur_radius;
        auto shadow_rect = rect.inflated(inflate, inflate, inflate, inflate).translated(shadow.offset_x, shadow.offset_y);
        rect = rect.united(shadow_rect);
    }
    return rect;
}

CSSPixelRect PaintableBox::absolute_paint_rect() const
{
    if (!m_absolute_paint_rect.has_value())
        m_absolute_paint_rect = compute_absolute_paint_rect();
    return *m_absolute_paint_rect;
}

Optional<CSSPixelRect> PaintableBox::get_clip_rect() const
{
    auto clip = computed_values().clip();
    if (clip.is_rect() && layout_box().is_absolutely_positioned()) {
        auto border_box = absolute_border_box_rect();
        return clip.to_rect().resolved(layout_node(), border_box);
    }
    return {};
}

void PaintableBox::before_paint(PaintContext& context, [[maybe_unused]] PaintPhase phase) const
{
    if (!is_visible())
        return;

    apply_scroll_offset(context, phase);
    apply_clip_overflow_rect(context, phase);
}

void PaintableBox::after_paint(PaintContext& context, [[maybe_unused]] PaintPhase phase) const
{
    if (!is_visible())
        return;

    clear_clip_overflow_rect(context, phase);
    reset_scroll_offset(context, phase);
}

void PaintableBox::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    if (phase == PaintPhase::Background) {
        paint_backdrop_filter(context);
        paint_background(context);
        paint_box_shadow(context);
    }

    if (phase == PaintPhase::Border) {
        paint_border(context);
    }

    if (phase == PaintPhase::Outline) {
        auto outline_width = computed_values().outline_width().to_px(layout_node());
        auto borders_data = borders_data_for_outline(layout_node(), computed_values().outline_color(), computed_values().outline_style(), outline_width);
        if (borders_data.has_value()) {
            auto outline_offset = computed_values().outline_offset().to_px(layout_node());
            auto border_radius_data = normalized_border_radii_data(ShrinkRadiiForBorders::No);
            auto borders_rect = absolute_border_box_rect();

            auto outline_offset_x = outline_offset;
            auto outline_offset_y = outline_offset;
            // "Both the height and the width of the outside of the shape drawn by the outline should not
            // become smaller than twice the computed value of the outline-width property to make sure
            // that an outline can be rendered even with large negative values."
            // https://www.w3.org/TR/css-ui-4/#outline-offset
            // So, if the horizontal outline offset is > half the borders_rect's width then we set it to that.
            // (And the same for y)
            if ((borders_rect.width() / 2) + outline_offset_x < 0)
                outline_offset_x = -borders_rect.width() / 2;
            if ((borders_rect.height() / 2) + outline_offset_y < 0)
                outline_offset_y = -borders_rect.height() / 2;

            border_radius_data.inflate(outline_width + outline_offset_y, outline_width + outline_offset_x, outline_width + outline_offset_y, outline_width + outline_offset_x);
            borders_rect.inflate(outline_width + outline_offset_y, outline_width + outline_offset_x, outline_width + outline_offset_y, outline_width + outline_offset_x);

            context.recording_painter().paint_borders(context.rounded_device_rect(borders_rect), border_radius_data.as_corners(context), borders_data->to_device_pixels(context));
        }
    }

    if (phase == PaintPhase::Overlay && layout_box().document().inspected_layout_node() == &layout_box()) {
        auto content_rect = absolute_rect();

        auto margin_box = box_model().margin_box();
        CSSPixelRect margin_rect;
        margin_rect.set_x(absolute_x() - margin_box.left);
        margin_rect.set_width(content_width() + margin_box.left + margin_box.right);
        margin_rect.set_y(absolute_y() - margin_box.top);
        margin_rect.set_height(content_height() + margin_box.top + margin_box.bottom);

        auto border_rect = absolute_border_box_rect();
        auto padding_rect = absolute_padding_box_rect();

        auto paint_inspector_rect = [&](CSSPixelRect const& rect, Color color) {
            auto device_rect = context.enclosing_device_rect(rect).to_type<int>();
            context.recording_painter().fill_rect(device_rect, Color(color).with_alpha(100));
            context.recording_painter().draw_rect(device_rect, Color(color));
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
        auto size_text = MUST(builder.to_string());
        auto size_text_rect = border_rect;
        size_text_rect.set_y(border_rect.y() + border_rect.height());
        size_text_rect.set_top(size_text_rect.top());
        size_text_rect.set_width(CSSPixels::nearest_value_for(font.width(size_text)) + 4);
        size_text_rect.set_height(CSSPixels::nearest_value_for(font.pixel_size()) + 4);
        auto size_text_device_rect = context.enclosing_device_rect(size_text_rect).to_type<int>();
        context.recording_painter().fill_rect(size_text_device_rect, context.palette().color(Gfx::ColorRole::Tooltip));
        context.recording_painter().draw_rect(size_text_device_rect, context.palette().threed_shadow1());
        context.recording_painter().draw_text(size_text_device_rect, size_text, font, Gfx::TextAlignment::Center, context.palette().color(Gfx::ColorRole::TooltipText));
    }
}

BordersData PaintableBox::remove_element_kind_from_borders_data(PaintableBox::BordersDataWithElementKind borders_data)
{
    return {
        .top = borders_data.top.border_data,
        .right = borders_data.right.border_data,
        .bottom = borders_data.bottom.border_data,
        .left = borders_data.left.border_data,
    };
}

void PaintableBox::paint_border(PaintContext& context) const
{
    auto borders_data = m_override_borders_data.has_value() ? remove_element_kind_from_borders_data(m_override_borders_data.value()) : BordersData {
        .top = box_model().border.top == 0 ? CSS::BorderData() : computed_values().border_top(),
        .right = box_model().border.right == 0 ? CSS::BorderData() : computed_values().border_right(),
        .bottom = box_model().border.bottom == 0 ? CSS::BorderData() : computed_values().border_bottom(),
        .left = box_model().border.left == 0 ? CSS::BorderData() : computed_values().border_left(),
    };
    context.recording_painter().paint_borders(context.rounded_device_rect(absolute_border_box_rect()), normalized_border_radii_data().as_corners(context), borders_data.to_device_pixels(context));
}

void PaintableBox::paint_backdrop_filter(PaintContext& context) const
{
    auto& backdrop_filter = computed_values().backdrop_filter();
    if (!backdrop_filter.is_none())
        apply_backdrop_filter(context, absolute_border_box_rect(), normalized_border_radii_data(), backdrop_filter);
}

void PaintableBox::paint_background(PaintContext& context) const
{
    // If the body's background properties were propagated to the root element, do no re-paint the body's background.
    if (layout_box().is_body() && document().html_element()->should_use_body_background_properties())
        return;

    CSSPixelRect background_rect;
    Color background_color = computed_values().background_color();
    auto* background_layers = &computed_values().background_layers();

    if (layout_box().is_root_element()) {
        // CSS 2.1 Appendix E.2: If the element is a root element, paint the background over the entire canvas.
        background_rect = context.css_viewport_rect();

        // Section 2.11.2: If the computed value of background-image on the root element is none and its background-color is transparent,
        // user agents must instead propagate the computed values of the background properties from that element’s first HTML BODY child element.
        if (document().html_element()->should_use_body_background_properties()) {
            background_layers = document().background_layers();
            background_color = document().background_color();
        }
    } else {
        background_rect = absolute_padding_box_rect();
    }

    // HACK: If the Box has a border, use the bordered_rect to paint the background.
    //       This way if we have a border-radius there will be no gap between the filling and actual border.
    if (computed_values().border_top().width != 0 || computed_values().border_right().width != 0 || computed_values().border_bottom().width != 0 || computed_values().border_left().width != 0)
        background_rect = absolute_border_box_rect();

    Painting::paint_background(context, layout_box(), background_rect, background_color, computed_values().image_rendering(), background_layers, normalized_border_radii_data());
}

void PaintableBox::paint_box_shadow(PaintContext& context) const
{
    auto const& resolved_box_shadow_data = box_shadow_data();
    if (resolved_box_shadow_data.is_empty())
        return;
    auto borders_data = BordersData {
        .top = computed_values().border_top(),
        .right = computed_values().border_right(),
        .bottom = computed_values().border_bottom(),
        .left = computed_values().border_left(),
    };
    Painting::paint_box_shadow(context, absolute_border_box_rect(), absolute_padding_box_rect(),
        borders_data, normalized_border_radii_data(), resolved_box_shadow_data);
}

BorderRadiiData PaintableBox::normalized_border_radii_data(ShrinkRadiiForBorders shrink) const
{
    auto border_radii_data = this->border_radii_data();
    if (shrink == ShrinkRadiiForBorders::Yes)
        border_radii_data.shrink(computed_values().border_top().width, computed_values().border_right().width, computed_values().border_bottom().width, computed_values().border_left().width);
    return border_radii_data;
}

void PaintableBox::apply_scroll_offset(PaintContext& context, PaintPhase) const
{
    if (m_scroll_frame_id.has_value()) {
        context.recording_painter().save();
        context.recording_painter().set_scroll_frame_id(m_scroll_frame_id.value());
    }
}

void PaintableBox::reset_scroll_offset(PaintContext& context, PaintPhase) const
{
    if (m_scroll_frame_id.has_value())
        context.recording_painter().restore();
}

void PaintableBox::apply_clip_overflow_rect(PaintContext& context, PaintPhase phase) const
{
    if (!AK::first_is_one_of(phase, PaintPhase::Background, PaintPhase::Border, PaintPhase::Foreground, PaintPhase::Outline))
        return;

    if (m_clip_rect.has_value()) {
        auto overflow_clip_rect = m_clip_rect.value();
        for (auto const* ancestor = &this->layout_box(); ancestor; ancestor = ancestor->containing_block()) {
            auto affine_transform = Gfx::extract_2d_affine_transform(ancestor->paintable_box()->transform());
            if (!affine_transform.is_identity()) {
                // NOTE: Because the painting command executor applies CSS transform of the nearest stacking context
                //       and the m_clip_rect is determined considering CSS transforms, here transform of the nearest
                //       stacking context need to be compensated.
                //       This adjustment ensures the transform is accounted for just once.
                overflow_clip_rect.translate_by(-affine_transform.translation().to_type<CSSPixels>());
                break;
            }
        }

        m_clipping_overflow = true;
        context.recording_painter().save();
        context.recording_painter().add_clip_rect(context.enclosing_device_rect(overflow_clip_rect).to_type<int>());
        if (m_corner_clip_radii.has_value()) {
            VERIFY(!m_corner_clipper_id.has_value());
            m_corner_clipper_id = context.allocate_corner_clipper_id();
            context.recording_painter().sample_under_corners(*m_corner_clipper_id, *m_corner_clip_radii, context.rounded_device_rect(overflow_clip_rect).to_type<int>(), CornerClip::Outside);
        }
    }
}

void PaintableBox::clear_clip_overflow_rect(PaintContext& context, PaintPhase phase) const
{
    if (!AK::first_is_one_of(phase, PaintPhase::Background, PaintPhase::Border, PaintPhase::Foreground, PaintPhase::Outline))
        return;

    if (m_clipping_overflow) {
        m_clipping_overflow = false;
        if (m_corner_clip_radii.has_value()) {
            VERIFY(m_corner_clipper_id.has_value());
            context.recording_painter().blit_corner_clipping(*m_corner_clipper_id, context.rounded_device_rect(*m_clip_rect).to_type<int>());
            m_corner_clipper_id = {};
        }
        context.recording_painter().restore();
    }
}

void paint_cursor_if_needed(PaintContext& context, Layout::TextNode const& text_node, PaintableFragment const& fragment)
{
    auto const& browsing_context = text_node.browsing_context();

    if (!browsing_context.is_focused_context())
        return;

    if (!browsing_context.cursor_blink_state())
        return;

    if (browsing_context.cursor_position()->node() != &text_node.dom_node())
        return;

    // NOTE: This checks if the cursor is before the start or after the end of the fragment. If it is at the end, after all text, it should still be painted.
    if (browsing_context.cursor_position()->offset() < (unsigned)fragment.start() || browsing_context.cursor_position()->offset() > (unsigned)(fragment.start() + fragment.length()))
        return;

    if (!fragment.layout_node().dom_node() || !fragment.layout_node().dom_node()->is_editable())
        return;

    auto fragment_rect = fragment.absolute_rect();

    auto text = text_node.text_for_rendering().bytes_as_string_view().substring_view(fragment.start(), fragment.length());
    CSSPixelRect cursor_rect {
        fragment_rect.x() + CSSPixels::nearest_value_for(text_node.first_available_font().width(text.substring_view(0, text_node.browsing_context().cursor_position()->offset() - fragment.start()))),
        fragment_rect.top(),
        1,
        fragment_rect.height()
    };

    auto cursor_device_rect = context.rounded_device_rect(cursor_rect).to_type<int>();

    context.recording_painter().draw_rect(cursor_device_rect, text_node.computed_values().color());
}

void paint_text_decoration(PaintContext& context, Layout::Node const& text_node, PaintableFragment const& fragment)
{
    auto& painter = context.recording_painter();
    auto& font = fragment.layout_node().first_available_font();
    auto fragment_box = fragment.absolute_rect();
    CSSPixels glyph_height = CSSPixels::nearest_value_for(font.pixel_size());
    auto baseline = fragment.baseline();

    auto line_color = text_node.computed_values().text_decoration_color();
    auto const& text_paintable = static_cast<TextPaintable const&>(fragment.paintable());
    auto device_line_thickness = context.rounded_device_pixels(text_paintable.text_decoration_thickness());

    auto text_decoration_lines = text_node.computed_values().text_decoration_line();
    for (auto line : text_decoration_lines) {
        DevicePixelPoint line_start_point {};
        DevicePixelPoint line_end_point {};

        switch (line) {
        case CSS::TextDecorationLine::None:
            return;
        case CSS::TextDecorationLine::Underline:
            line_start_point = context.rounded_device_point(fragment_box.top_left().translated(0, baseline + 2));
            line_end_point = context.rounded_device_point(fragment_box.top_right().translated(-1, baseline + 2));
            break;
        case CSS::TextDecorationLine::Overline:
            line_start_point = context.rounded_device_point(fragment_box.top_left().translated(0, baseline - glyph_height));
            line_end_point = context.rounded_device_point(fragment_box.top_right().translated(-1, baseline - glyph_height));
            break;
        case CSS::TextDecorationLine::LineThrough: {
            auto x_height = font.x_height();
            line_start_point = context.rounded_device_point(fragment_box.top_left().translated(0, baseline - x_height * CSSPixels(0.5f)));
            line_end_point = context.rounded_device_point(fragment_box.top_right().translated(-1, baseline - x_height * CSSPixels(0.5f)));
            break;
        }
        case CSS::TextDecorationLine::Blink:
            // Conforming user agents may simply not blink the text
            return;
        }

        switch (text_node.computed_values().text_decoration_style()) {
        case CSS::TextDecorationStyle::Solid:
            painter.draw_line(line_start_point.to_type<int>(), line_end_point.to_type<int>(), line_color, device_line_thickness.value(), Gfx::Painter::LineStyle::Solid);
            break;
        case CSS::TextDecorationStyle::Double:
            switch (line) {
            case CSS::TextDecorationLine::Underline:
                break;
            case CSS::TextDecorationLine::Overline:
                line_start_point.translate_by(0, -device_line_thickness - context.rounded_device_pixels(1));
                line_end_point.translate_by(0, -device_line_thickness - context.rounded_device_pixels(1));
                break;
            case CSS::TextDecorationLine::LineThrough:
                line_start_point.translate_by(0, -device_line_thickness / 2);
                line_end_point.translate_by(0, -device_line_thickness / 2);
                break;
            default:
                VERIFY_NOT_REACHED();
            }

            painter.draw_line(line_start_point.to_type<int>(), line_end_point.to_type<int>(), line_color, device_line_thickness.value());
            painter.draw_line(line_start_point.translated(0, device_line_thickness + 1).to_type<int>(), line_end_point.translated(0, device_line_thickness + 1).to_type<int>(), line_color, device_line_thickness.value());
            break;
        case CSS::TextDecorationStyle::Dashed:
            painter.draw_line(line_start_point.to_type<int>(), line_end_point.to_type<int>(), line_color, device_line_thickness.value(), Gfx::Painter::LineStyle::Dashed);
            break;
        case CSS::TextDecorationStyle::Dotted:
            painter.draw_line(line_start_point.to_type<int>(), line_end_point.to_type<int>(), line_color, device_line_thickness.value(), Gfx::Painter::LineStyle::Dotted);
            break;
        case CSS::TextDecorationStyle::Wavy:
            painter.draw_triangle_wave(line_start_point.to_type<int>(), line_end_point.to_type<int>(), line_color, device_line_thickness.value() + 1, device_line_thickness.value());
            break;
        }
    }
}

void paint_text_fragment(PaintContext& context, Layout::TextNode const& text_node, PaintableFragment const& fragment, PaintPhase phase)
{
    auto& painter = context.recording_painter();

    if (phase == PaintPhase::Foreground) {
        auto fragment_absolute_rect = fragment.absolute_rect();
        auto fragment_absolute_device_rect = context.enclosing_device_rect(fragment_absolute_rect);

        if (text_node.document().inspected_layout_node() == &text_node)
            context.recording_painter().draw_rect(fragment_absolute_device_rect.to_type<int>(), Color::Magenta);

        auto text = text_node.text_for_rendering();

        DevicePixelPoint baseline_start { fragment_absolute_device_rect.x(), fragment_absolute_device_rect.y() + context.rounded_device_pixels(fragment.baseline()) };
        Vector<Gfx::DrawGlyphOrEmoji> scaled_glyph_run;
        scaled_glyph_run.ensure_capacity(fragment.glyph_run().size());
        for (auto glyph : fragment.glyph_run()) {
            glyph.visit([&](auto& glyph) {
                glyph.font = *glyph.font->with_size(glyph.font->point_size() * static_cast<float>(context.device_pixels_per_css_pixel()));
                glyph.position = glyph.position.scaled(context.device_pixels_per_css_pixel());
            });
            scaled_glyph_run.append(move(glyph));
        }
        painter.draw_text_run(baseline_start.to_type<int>(), scaled_glyph_run, text_node.computed_values().color(), fragment_absolute_device_rect.to_type<int>());

        auto selection_rect = context.enclosing_device_rect(fragment.selection_rect(text_node.first_available_font())).to_type<int>();
        if (!selection_rect.is_empty()) {
            painter.fill_rect(selection_rect, CSS::SystemColor::highlight());
            RecordingPainterStateSaver saver(painter);
            painter.add_clip_rect(selection_rect);
            painter.draw_text_run(baseline_start.to_type<int>(), scaled_glyph_run, CSS::SystemColor::highlight_text(), fragment_absolute_device_rect.to_type<int>());
        }

        paint_text_decoration(context, text_node, fragment);
        paint_cursor_if_needed(context, text_node, fragment);
    }
}

void PaintableWithLines::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    PaintableBox::paint(context, phase);

    if (fragments().is_empty())
        return;

    bool should_clip_overflow = computed_values().overflow_x() != CSS::Overflow::Visible && computed_values().overflow_y() != CSS::Overflow::Visible;
    Optional<u32> corner_clip_id;

    auto clip_box = context.rounded_device_rect(absolute_padding_box_rect());

    if (should_clip_overflow) {
        context.recording_painter().save();
        // FIXME: Handle overflow-x and overflow-y being different values.
        context.recording_painter().add_clip_rect(clip_box.to_type<int>());
        auto scroll_offset = context.rounded_device_point(this->scroll_offset());
        context.recording_painter().translate(-scroll_offset.to_type<int>());

        auto border_radii = normalized_border_radii_data(ShrinkRadiiForBorders::Yes);
        CornerRadii corner_radii {
            .top_left = border_radii.top_left.as_corner(context),
            .top_right = border_radii.top_right.as_corner(context),
            .bottom_right = border_radii.bottom_right.as_corner(context),
            .bottom_left = border_radii.bottom_left.as_corner(context)
        };
        if (corner_radii.has_any_radius()) {
            corner_clip_id = context.allocate_corner_clipper_id();
            context.recording_painter().sample_under_corners(*corner_clip_id, corner_radii, clip_box.to_type<int>(), CornerClip::Outside);
        }
    }

    // Text shadows
    // This is yet another loop, but done here because all shadows should appear under all text.
    // So, we paint the shadows before painting any text.
    // FIXME: Find a smarter way to do this?
    if (phase == PaintPhase::Foreground) {
        for (auto& fragment : fragments()) {
            if (fragment.contained_by_inline_node())
                continue;
            paint_text_shadow(context, fragment, fragment.shadows());
        }
    }

    for (auto const& fragment : m_fragments) {
        if (fragment.contained_by_inline_node())
            continue;
        auto fragment_absolute_rect = fragment.absolute_rect();
        auto fragment_absolute_device_rect = context.enclosing_device_rect(fragment_absolute_rect);
        if (context.should_show_line_box_borders()) {
            context.recording_painter().draw_rect(fragment_absolute_device_rect.to_type<int>(), Color::Green);
            context.recording_painter().draw_line(
                context.rounded_device_point(fragment_absolute_rect.top_left().translated(0, fragment.baseline())).to_type<int>(),
                context.rounded_device_point(fragment_absolute_rect.top_right().translated(-1, fragment.baseline())).to_type<int>(), Color::Red);
        }
        if (is<Layout::TextNode>(fragment.layout_node()))
            paint_text_fragment(context, static_cast<Layout::TextNode const&>(fragment.layout_node()), fragment, phase);
    }

    if (should_clip_overflow) {
        context.recording_painter().restore();
        if (corner_clip_id.has_value()) {
            context.recording_painter().blit_corner_clipping(*corner_clip_id, clip_box.to_type<int>());
            corner_clip_id = {};
        }
    }
}

bool PaintableBox::handle_mousewheel(Badge<EventHandler>, CSSPixelPoint, unsigned, unsigned, int wheel_delta_x, int wheel_delta_y)
{
    if (!layout_box().is_user_scrollable())
        return false;
    scroll_by(wheel_delta_x, wheel_delta_y);
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

Optional<HitTestResult> PaintableBox::hit_test(CSSPixelPoint position, HitTestType type) const
{
    if (!is_visible())
        return {};

    if (layout_box().is_viewport()) {
        const_cast<ViewportPaintable&>(static_cast<ViewportPaintable const&>(*this)).build_stacking_context_tree_if_needed();
        return stacking_context()->hit_test(position, type);
    }

    if (!absolute_border_box_rect().contains(position.x(), position.y()))
        return {};

    for (auto* child = first_child(); child; child = child->next_sibling()) {
        auto result = child->hit_test(position, type);
        if (!result.has_value())
            continue;
        if (!result->paintable->visible_for_hit_testing())
            continue;
        return result;
    }

    if (!visible_for_hit_testing())
        return {};

    return HitTestResult { const_cast<PaintableBox&>(*this) };
}

Optional<HitTestResult> PaintableWithLines::hit_test(CSSPixelPoint position, HitTestType type) const
{
    if (!layout_box().children_are_inline() || m_fragments.is_empty())
        return PaintableBox::hit_test(position, type);

    for (auto* child = first_child(); child; child = child->next_sibling()) {
        auto result = child->hit_test(position, type);
        if (!result.has_value())
            continue;
        if (!result->paintable->visible_for_hit_testing())
            continue;
        return result;
    }

    Optional<HitTestResult> last_good_candidate;
    for (auto const& fragment : fragments()) {
        if (fragment.paintable().stacking_context())
            continue;
        auto fragment_absolute_rect = fragment.absolute_rect();
        if (fragment_absolute_rect.contains(position)) {
            if (auto result = fragment.paintable().hit_test(position, type); result.has_value())
                return result;
            return HitTestResult { const_cast<Paintable&>(fragment.paintable()), fragment.text_index_at(position.x()) };
        }

        // If we reached this point, the position is not within the fragment. However, the fragment start or end might be the place to place the cursor.
        // This determines whether the fragment is a good candidate for the position. The last such good fragment is chosen.
        // The best candidate is either the end of the line above, the beginning of the line below, or the beginning or end of the current line.
        // We arbitrarily choose to consider the end of the line above and ignore the beginning of the line below.
        // If we knew the direction of selection, we could make a better choice.
        if (fragment_absolute_rect.bottom() - 1 <= position.y()) { // fully below the fragment
            last_good_candidate = HitTestResult { const_cast<Paintable&>(fragment.paintable()), fragment.start() + fragment.length() };
        } else if (fragment_absolute_rect.top() <= position.y()) { // vertically within the fragment
            if (position.x() < fragment_absolute_rect.left()) {    // left of the fragment
                if (!last_good_candidate.has_value()) {            // first fragment of the line
                    last_good_candidate = HitTestResult { const_cast<Paintable&>(fragment.paintable()), fragment.start() };
                }
            } else { // right of the fragment
                last_good_candidate = HitTestResult { const_cast<Paintable&>(fragment.paintable()), fragment.start() + fragment.length() };
            }
        }
    }

    if (type == HitTestType::TextCursor && last_good_candidate.has_value())
        return last_good_candidate;
    if (is_visible() && absolute_border_box_rect().contains(position.x(), position.y()))
        return HitTestResult { const_cast<PaintableWithLines&>(*this) };
    return {};
}

void PaintableBox::set_needs_display() const
{
    if (auto navigable = this->navigable())
        navigable->set_needs_display(absolute_rect());
}

}
