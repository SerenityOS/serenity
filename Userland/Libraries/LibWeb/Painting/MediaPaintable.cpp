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
    auto maximum_control_box_size = context.rounded_device_pixels(40);
    auto playback_padding = context.rounded_device_pixels(5);

    auto control_box_rect = media_rect;
    if (control_box_rect.height() > maximum_control_box_size)
        control_box_rect.take_from_top(control_box_rect.height() - maximum_control_box_size);

    context.painter().fill_rect(control_box_rect.to_type<int>(), control_box_color.with_alpha(0xd0));
    media_element.cached_layout_boxes({}).control_box_rect = context.scale_to_css_rect(control_box_rect);

    control_box_rect = paint_control_bar_playback_button(context, media_element, control_box_rect, mouse_position);
    control_box_rect.take_from_left(playback_padding);

    control_box_rect = paint_control_bar_timeline(context, media_element, control_box_rect, mouse_position);
    control_box_rect.take_from_left(playback_padding);

    control_box_rect = paint_control_bar_timestamp(context, media_element, control_box_rect);
    control_box_rect.take_from_left(playback_padding);
}

DevicePixelRect MediaPaintable::paint_control_bar_playback_button(PaintContext& context, HTML::HTMLMediaElement const& media_element, DevicePixelRect control_box_rect, Optional<DevicePixelPoint> const& mouse_position) const
{
    auto maximum_playback_button_size = context.rounded_device_pixels(15);
    auto maximum_playback_button_offset_x = context.rounded_device_pixels(15);

    auto playback_button_size = min(maximum_playback_button_size, control_box_rect.height() / 2);
    auto playback_button_offset_x = min(maximum_playback_button_offset_x, control_box_rect.width());
    auto playback_button_offset_y = (control_box_rect.height() - playback_button_size) / 2;

    auto playback_button_location = control_box_rect.top_left().translated(playback_button_offset_x, playback_button_offset_y);

    auto playback_button_hover_rect = DevicePixelRect {
        control_box_rect.top_left(),
        { playback_button_size + playback_button_offset_x * 2, control_box_rect.height() }
    };
    media_element.cached_layout_boxes({}).playback_button_rect = context.scale_to_css_rect(playback_button_hover_rect);

    auto playback_button_is_hovered = mouse_position.has_value() && playback_button_hover_rect.contains(*mouse_position);
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
            { maximum_playback_button_size / 3, playback_button_size }
        };
        DevicePixelRect pause_button_right_rect {
            playback_button_location.translated(maximum_playback_button_size * 2 / 3, 0),
            { maximum_playback_button_size / 3, playback_button_size }
        };

        context.painter().fill_rect(pause_button_left_rect.to_type<int>(), playback_button_color);
        context.painter().fill_rect(pause_button_right_rect.to_type<int>(), playback_button_color);
    }

    control_box_rect.take_from_left(playback_button_hover_rect.width());
    return control_box_rect;
}

DevicePixelRect MediaPaintable::paint_control_bar_timeline(PaintContext& context, HTML::HTMLMediaElement const& media_element, DevicePixelRect control_box_rect, Optional<DevicePixelPoint> const& mouse_position) const
{
    auto maximum_timeline_button_size = context.rounded_device_pixels(16);

    auto timeline_rect = control_box_rect;
    if (is<HTML::HTMLAudioElement>(media_element))
        timeline_rect.set_width(min(control_box_rect.width() * 6 / 10, timeline_rect.width() * 4 / 10));
    else
        timeline_rect.set_width(min(control_box_rect.width() * 6 / 10, timeline_rect.width()));
    media_element.cached_layout_boxes({}).timeline_rect = context.scale_to_css_rect(timeline_rect);

    auto playback_percentage = media_element.current_time() / media_element.duration();
    auto playback_position = static_cast<double>(static_cast<int>(timeline_rect.width())) * playback_percentage;

    auto timeline_button_size = min(maximum_timeline_button_size, timeline_rect.height() / 2);
    auto timeline_button_offset_x = static_cast<DevicePixels>(round(playback_position));

    Gfx::AntiAliasingPainter painter { context.painter() };

    auto playback_timelime_scrub_rect = timeline_rect;
    playback_timelime_scrub_rect.shrink(0, timeline_rect.height() - timeline_button_size / 2);

    auto timeline_past_rect = playback_timelime_scrub_rect;
    timeline_past_rect.set_width(timeline_button_offset_x);
    painter.fill_rect_with_rounded_corners(timeline_past_rect.to_type<int>(), control_highlight_color.lightened(), 4);

    auto timeline_future_rect = playback_timelime_scrub_rect;
    timeline_future_rect.take_from_left(timeline_button_offset_x);
    painter.fill_rect_with_rounded_corners(timeline_future_rect.to_type<int>(), Color::Black, 4);

    auto timeline_button_rect = timeline_rect;
    timeline_button_rect.shrink(timeline_rect.width() - timeline_button_size, timeline_rect.height() - timeline_button_size);
    timeline_button_rect.set_x(timeline_rect.x() + timeline_button_offset_x - timeline_button_size / 2);

    auto timeline_is_hovered = mouse_position.has_value() && timeline_rect.contains(*mouse_position);
    auto timeline_color = control_button_color(timeline_is_hovered);
    painter.fill_ellipse(timeline_button_rect.to_type<int>(), timeline_color);

    control_box_rect.take_from_left(timeline_rect.width() + timeline_button_size / 2);
    return control_box_rect;
}

DevicePixelRect MediaPaintable::paint_control_bar_timestamp(PaintContext& context, HTML::HTMLMediaElement const& media_element, DevicePixelRect control_box_rect) const
{
    auto current_time = human_readable_digital_time(round(media_element.current_time()));
    auto duration = human_readable_digital_time(round(media_element.duration()));
    auto timestamp = String::formatted("{} / {}", current_time, duration).release_value_but_fixme_should_propagate_errors();

    auto const& scaled_font = layout_node().scaled_font(context);
    auto font = scaled_font.with_size(10);
    if (!font)
        font = scaled_font;

    auto timestamp_size = static_cast<DevicePixels::Type>(ceilf(font->width(timestamp)));
    if (timestamp_size > control_box_rect.width())
        return control_box_rect;

    auto timestamp_rect = control_box_rect;
    timestamp_rect.set_width(timestamp_size);
    context.painter().draw_text(timestamp_rect.to_type<int>(), timestamp, *font, Gfx::TextAlignment::CenterLeft, Color::White);

    control_box_rect.take_from_left(timestamp_rect.width());
    return control_box_rect;
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
