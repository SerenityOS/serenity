/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/NumberFormat.h>
#include <LibGUI/Event.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLAudioElement.h>
#include <LibWeb/HTML/HTMLMediaElement.h>
#include <LibWeb/HTML/HTMLVideoElement.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Painting/MediaPaintable.h>

namespace Web::Painting {

static constexpr auto control_box_color = Gfx::Color::from_rgb(0x26'26'26);
static constexpr auto control_highlight_color = Gfx::Color::from_rgb(0x1d'99'f3);

static constexpr Gfx::Color control_button_color(bool is_hovered)
{
    if (!is_hovered)
        return Color::White;
    return control_highlight_color;
}

MediaPaintable::MediaPaintable(Layout::ReplacedBox const& layout_box)
    : PaintableBox(layout_box)
{
}

Optional<DevicePixelPoint> MediaPaintable::mouse_position(PaintContext& context, HTML::HTMLMediaElement const& media_element)
{
    auto const& layout_mouse_position = media_element.layout_mouse_position({});

    if (layout_mouse_position.has_value() && media_element.document().hovered_node() == &media_element)
        return context.rounded_device_point(*layout_mouse_position);

    return {};
}

void MediaPaintable::fill_triangle(Gfx::Painter& painter, Gfx::IntPoint location, Array<Gfx::IntPoint, 3> coordinates, Color color)
{
    Gfx::AntiAliasingPainter aa_painter { painter };
    Gfx::Path path;
    path.move_to((coordinates[0] + location).to_type<float>());
    path.line_to((coordinates[1] + location).to_type<float>());
    path.line_to((coordinates[2] + location).to_type<float>());
    path.close();
    aa_painter.fill_path(path, color, Gfx::Painter::WindingRule::EvenOdd);
}

void MediaPaintable::paint_media_controls(PaintContext& context, HTML::HTMLMediaElement const& media_element, DevicePixelRect media_rect, Optional<DevicePixelPoint> const& mouse_position) const
{
    auto components = compute_control_bar_components(context, media_element, media_rect);
    context.painter().fill_rect(components.control_box_rect.to_type<int>(), control_box_color.with_alpha(0xd0));

    paint_control_bar_playback_button(context, media_element, components, mouse_position);
    paint_control_bar_timeline(context, media_element, components, mouse_position);
    paint_control_bar_timestamp(context, components);
    paint_control_bar_speaker(context, media_element, components, mouse_position);
}

MediaPaintable::Components MediaPaintable::compute_control_bar_components(PaintContext& context, HTML::HTMLMediaElement const& media_element, DevicePixelRect media_rect) const
{
    auto maximum_control_box_height = context.rounded_device_pixels(40);
    auto component_padding = context.rounded_device_pixels(5);

    Components components {};

    components.control_box_rect = media_rect;
    if (components.control_box_rect.height() > maximum_control_box_height)
        components.control_box_rect.take_from_top(components.control_box_rect.height() - maximum_control_box_height);

    auto remaining_rect = components.control_box_rect;
    remaining_rect.shrink(component_padding * 2, 0);

    auto playback_button_rect_width = min(context.rounded_device_pixels(40), remaining_rect.width());
    components.playback_button_rect = remaining_rect;
    components.playback_button_rect.set_width(playback_button_rect_width);
    remaining_rect.take_from_left(playback_button_rect_width);

    components.speaker_button_size = context.rounded_device_pixels(30);
    if (components.speaker_button_size <= remaining_rect.width()) {
        components.speaker_button_rect = remaining_rect;
        components.speaker_button_rect.take_from_left(remaining_rect.width() - components.speaker_button_size);
        remaining_rect.take_from_right(components.speaker_button_size + component_padding);
    }

    auto current_time = human_readable_digital_time(round(media_element.current_time()));
    auto duration = human_readable_digital_time(isnan(media_element.duration()) ? 0 : round(media_element.duration()));
    components.timestamp = String::formatted("{} / {}", current_time, duration).release_value_but_fixme_should_propagate_errors();

    auto const& scaled_font = layout_node().scaled_font(context);
    components.timestamp_font = scaled_font.with_size(10);
    if (!components.timestamp_font)
        components.timestamp_font = scaled_font;

    auto timestamp_size = DevicePixels { static_cast<DevicePixels::Type>(ceilf(components.timestamp_font->width(components.timestamp))) };
    if (timestamp_size <= remaining_rect.width()) {
        components.timestamp_rect = remaining_rect;
        components.timestamp_rect.take_from_left(remaining_rect.width() - timestamp_size);
        remaining_rect.take_from_right(timestamp_size + component_padding);
    }

    components.timeline_button_size = context.rounded_device_pixels(16);
    if ((components.timeline_button_size * 3) <= remaining_rect.width())
        components.timeline_rect = remaining_rect;

    media_element.cached_layout_boxes({}).control_box_rect = context.scale_to_css_rect(components.control_box_rect);
    media_element.cached_layout_boxes({}).playback_button_rect = context.scale_to_css_rect(components.playback_button_rect);
    media_element.cached_layout_boxes({}).timeline_rect = context.scale_to_css_rect(components.timeline_rect);
    media_element.cached_layout_boxes({}).speaker_button_rect = context.scale_to_css_rect(components.speaker_button_rect);

    return components;
}

void MediaPaintable::paint_control_bar_playback_button(PaintContext& context, HTML::HTMLMediaElement const& media_element, Components const& components, Optional<DevicePixelPoint> const& mouse_position)
{
    auto playback_button_size = components.playback_button_rect.width() * 4 / 10;

    auto playback_button_offset_x = (components.playback_button_rect.width() - playback_button_size) / 2;
    auto playback_button_offset_y = (components.playback_button_rect.height() - playback_button_size) / 2;
    auto playback_button_location = components.playback_button_rect.top_left().translated(playback_button_offset_x, playback_button_offset_y);

    auto playback_button_is_hovered = mouse_position.has_value() && components.playback_button_rect.contains(*mouse_position);
    auto playback_button_color = control_button_color(playback_button_is_hovered);

    if (media_element.paused()) {
        Array<Gfx::IntPoint, 3> play_button_coordinates { {
            { 0, 0 },
            { static_cast<int>(playback_button_size), static_cast<int>(playback_button_size) / 2 },
            { 0, static_cast<int>(playback_button_size) },
        } };

        fill_triangle(context.painter(), playback_button_location.to_type<int>(), play_button_coordinates, playback_button_color);
    } else {
        DevicePixelRect pause_button_left_rect {
            playback_button_location,
            { playback_button_size / 3, playback_button_size }
        };
        DevicePixelRect pause_button_right_rect {
            playback_button_location.translated(playback_button_size * 2 / 3, 0),
            { playback_button_size / 3, playback_button_size }
        };

        context.painter().fill_rect(pause_button_left_rect.to_type<int>(), playback_button_color);
        context.painter().fill_rect(pause_button_right_rect.to_type<int>(), playback_button_color);
    }
}

void MediaPaintable::paint_control_bar_timeline(PaintContext& context, HTML::HTMLMediaElement const& media_element, Components const& components, Optional<DevicePixelPoint> const& mouse_position)
{
    if (components.timeline_rect.is_empty())
        return;

    auto timelime_scrub_rect = components.timeline_rect;
    timelime_scrub_rect.shrink(components.timeline_button_size, timelime_scrub_rect.height() - components.timeline_button_size / 2);

    auto playback_percentage = isnan(media_element.duration()) ? 0.0 : media_element.current_time() / media_element.duration();
    auto playback_position = static_cast<double>(static_cast<int>(timelime_scrub_rect.width())) * playback_percentage;
    auto timeline_button_offset_x = static_cast<DevicePixels>(round(playback_position));

    Gfx::AntiAliasingPainter painter { context.painter() };

    auto timeline_past_rect = timelime_scrub_rect;
    timeline_past_rect.set_width(timeline_button_offset_x);
    painter.fill_rect_with_rounded_corners(timeline_past_rect.to_type<int>(), control_highlight_color.lightened(), 4);

    auto timeline_future_rect = timelime_scrub_rect;
    timeline_future_rect.take_from_left(timeline_button_offset_x);
    painter.fill_rect_with_rounded_corners(timeline_future_rect.to_type<int>(), Color::Black, 4);

    auto timeline_button_rect = timelime_scrub_rect;
    timeline_button_rect.shrink(timelime_scrub_rect.width() - components.timeline_button_size, timelime_scrub_rect.height() - components.timeline_button_size);
    timeline_button_rect.set_x(timelime_scrub_rect.x() + timeline_button_offset_x - components.timeline_button_size / 2);

    auto timeline_is_hovered = mouse_position.has_value() && components.timeline_rect.contains(*mouse_position);
    auto timeline_color = control_button_color(timeline_is_hovered);
    painter.fill_ellipse(timeline_button_rect.to_type<int>(), timeline_color);
}

void MediaPaintable::paint_control_bar_timestamp(PaintContext& context, Components const& components)
{
    if (components.timestamp_rect.is_empty())
        return;

    context.painter().draw_text(components.timestamp_rect.to_type<int>(), components.timestamp, *components.timestamp_font, Gfx::TextAlignment::CenterLeft, Color::White);
}

void MediaPaintable::paint_control_bar_speaker(PaintContext& context, HTML::HTMLMediaElement const& media_element, Components const& components, Optional<DevicePixelPoint> const& mouse_position)
{
    if (components.speaker_button_rect.is_empty())
        return;

    auto speaker_button_width = context.rounded_device_pixels(20);
    auto speaker_button_height = context.rounded_device_pixels(15);

    auto speaker_button_offset_x = (components.speaker_button_rect.width() - speaker_button_width) / 2;
    auto speaker_button_offset_y = (components.speaker_button_rect.height() - speaker_button_height) / 2;
    auto speaker_button_location = components.speaker_button_rect.top_left().translated(speaker_button_offset_x, speaker_button_offset_y);

    auto device_point = [&](double x, double y) {
        auto position = context.rounded_device_point({ x, y }) + speaker_button_location;
        return position.to_type<DevicePixels::Type>().to_type<float>();
    };

    auto speaker_button_is_hovered = mouse_position.has_value() && components.speaker_button_rect.contains(*mouse_position);
    auto speaker_button_color = control_button_color(speaker_button_is_hovered);

    Gfx::AntiAliasingPainter painter { context.painter() };
    Gfx::Path path;

    path.move_to(device_point(0, 4));
    path.line_to(device_point(5, 4));
    path.line_to(device_point(11, 0));
    path.line_to(device_point(11, 15));
    path.line_to(device_point(5, 11));
    path.line_to(device_point(0, 11));
    path.line_to(device_point(0, 4));
    path.close();
    painter.fill_path(path, speaker_button_color, Gfx::Painter::WindingRule::EvenOdd);

    path.clear();
    path.move_to(device_point(13, 3));
    path.quadratic_bezier_curve_to(device_point(16, 7.5), device_point(13, 12));
    path.move_to(device_point(14, 0));
    path.quadratic_bezier_curve_to(device_point(20, 7.5), device_point(14, 15));
    painter.stroke_path(path, speaker_button_color, 1);

    if (media_element.muted()) {
        painter.draw_line(device_point(0, 0), device_point(20, 15), Color::Red, 2);
        painter.draw_line(device_point(0, 15), device_point(20, 0), Color::Red, 2);
    }
}

MediaPaintable::DispatchEventOfSameName MediaPaintable::handle_mouseup(Badge<EventHandler>, CSSPixelPoint position, unsigned button, unsigned)
{
    if (button != GUI::MouseButton::Primary)
        return DispatchEventOfSameName::Yes;

    auto& media_element = *verify_cast<HTML::HTMLMediaElement>(layout_box().dom_node());
    auto const& cached_layout_boxes = media_element.cached_layout_boxes({});

    // FIXME: This runs from outside the context of any user script, so we do not have a running execution
    //        context. This pushes one to allow the promise creation hook to run.
    auto& environment_settings = document().relevant_settings_object();
    environment_settings.prepare_to_run_script();

    ScopeGuard guard { [&] { environment_settings.clean_up_after_running_script(); } };

    auto toggle_playback = [&]() -> WebIDL::ExceptionOr<void> {
        if (media_element.paused())
            TRY(media_element.play());
        else
            TRY(media_element.pause());
        return {};
    };

    if (cached_layout_boxes.control_box_rect.has_value() && cached_layout_boxes.control_box_rect->contains(position)) {
        if (cached_layout_boxes.playback_button_rect.has_value() && cached_layout_boxes.playback_button_rect->contains(position)) {
            toggle_playback().release_value_but_fixme_should_propagate_errors();
            return DispatchEventOfSameName::Yes;
        }

        if (cached_layout_boxes.timeline_rect.has_value() && cached_layout_boxes.timeline_rect->contains(position)) {
            auto x_offset = position.x() - cached_layout_boxes.timeline_rect->x();
            auto x_percentage = static_cast<double>(x_offset) / static_cast<double>(cached_layout_boxes.timeline_rect->width());

            auto position = static_cast<double>(x_percentage) * media_element.duration();
            media_element.set_current_time(position);

            return DispatchEventOfSameName::Yes;
        }

        if (cached_layout_boxes.speaker_button_rect.has_value() && cached_layout_boxes.speaker_button_rect->contains(position)) {
            media_element.set_muted(!media_element.muted());
            return DispatchEventOfSameName::Yes;
        }

        return DispatchEventOfSameName::No;
    }

    toggle_playback().release_value_but_fixme_should_propagate_errors();
    return DispatchEventOfSameName::Yes;
}

MediaPaintable::DispatchEventOfSameName MediaPaintable::handle_mousemove(Badge<EventHandler>, CSSPixelPoint position, unsigned, unsigned)
{
    auto& media_element = *verify_cast<HTML::HTMLMediaElement>(layout_box().dom_node());

    if (absolute_rect().contains(position)) {
        media_element.set_layout_mouse_position({}, position);
        return DispatchEventOfSameName::Yes;
    }

    media_element.set_layout_mouse_position({}, {});
    return DispatchEventOfSameName::No;
}

}
