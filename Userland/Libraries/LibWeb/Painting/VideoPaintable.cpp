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
#include <LibWeb/HTML/HTMLMediaElement.h>
#include <LibWeb/HTML/HTMLVideoElement.h>
#include <LibWeb/Layout/VideoBox.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/InputColors.h>
#include <LibWeb/Painting/VideoPaintable.h>

namespace Web::Painting {

static constexpr auto control_box_color = Gfx::Color::from_rgb(0x26'26'26);
static constexpr auto control_highlight_color = Gfx::Color::from_rgb(0x1d'99'f3);

static constexpr Gfx::Color control_button_color(bool is_hovered)
{
    if (!is_hovered)
        return Color::White;
    return control_highlight_color;
}

JS::NonnullGCPtr<VideoPaintable> VideoPaintable::create(Layout::VideoBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<VideoPaintable>(layout_box);
}

VideoPaintable::VideoPaintable(Layout::VideoBox const& layout_box)
    : PaintableBox(layout_box)
{
}

Layout::VideoBox& VideoPaintable::layout_box()
{
    return static_cast<Layout::VideoBox&>(layout_node());
}

Layout::VideoBox const& VideoPaintable::layout_box() const
{
    return static_cast<Layout::VideoBox const&>(layout_node());
}

void VideoPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    // FIXME: This should be done at a different level.
    if (is_out_of_view(context))
        return;

    PaintableBox::paint(context, phase);

    if (phase != PaintPhase::Foreground)
        return;

    auto video_rect = context.rounded_device_rect(absolute_rect());
    context.painter().add_clip_rect(video_rect.to_type<int>());

    ScopedCornerRadiusClip corner_clip { context, context.painter(), video_rect, normalized_border_radii_data(ShrinkRadiiForBorders::Yes) };

    auto const& video_element = layout_box().dom_node();
    auto const& layout_mouse_position = video_element.layout_mouse_position({});

    Optional<DevicePixelPoint> mouse_position;
    if (layout_mouse_position.has_value() && document().hovered_node() == &video_element)
        mouse_position = context.rounded_device_point(*layout_mouse_position);

    auto paint_user_agent_controls = video_element.has_attribute(HTML::AttributeNames::controls);

    if (auto const& bitmap = layout_box().dom_node().current_frame()) {
        context.painter().draw_scaled_bitmap(video_rect.to_type<int>(), *bitmap, bitmap->rect(), 1.0f, to_gfx_scaling_mode(computed_values().image_rendering()));

        if (paint_user_agent_controls)
            paint_loaded_video_controls(context, video_element, video_rect, mouse_position);
    } else {
        auto input_colors = compute_input_colors(context.palette(), computed_values().accent_color());
        context.painter().fill_rect(video_rect.to_type<int>(), input_colors.light_gray);

        if (paint_user_agent_controls)
            paint_placeholder_video_controls(context, video_rect, mouse_position);
    }
}

void VideoPaintable::paint_loaded_video_controls(PaintContext& context, HTML::HTMLVideoElement const& video_element, DevicePixelRect video_rect, Optional<DevicePixelPoint> const& mouse_position) const
{
    auto maximum_control_box_size = context.rounded_device_pixels(30);
    auto playback_padding = context.rounded_device_pixels(5);

    auto is_hovered = document().hovered_node() == &video_element;
    auto is_paused = video_element.paused();

    if (!is_hovered && !is_paused)
        return;

    auto control_box_rect = video_rect;
    if (control_box_rect.height() > maximum_control_box_size)
        control_box_rect.take_from_top(control_box_rect.height() - maximum_control_box_size);

    context.painter().fill_rect(control_box_rect.to_type<int>(), control_box_color.with_alpha(0xd0));
    layout_box().dom_node().cached_layout_boxes({}).control_box_rect = context.scale_to_css_rect(control_box_rect);

    control_box_rect = paint_control_bar_playback_button(context, video_element, control_box_rect, mouse_position);
    control_box_rect.take_from_left(playback_padding);

    control_box_rect = paint_control_bar_timeline(context, video_element, control_box_rect, mouse_position);
    control_box_rect.take_from_left(playback_padding);

    control_box_rect = paint_control_bar_timestamp(context, video_element, control_box_rect);
    control_box_rect.take_from_left(playback_padding);
}

DevicePixelRect VideoPaintable::paint_control_bar_playback_button(PaintContext& context, HTML::HTMLVideoElement const& video_element, DevicePixelRect control_box_rect, Optional<DevicePixelPoint> const& mouse_position) const
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
    layout_box().dom_node().cached_layout_boxes({}).playback_button_rect = context.scale_to_css_rect(playback_button_hover_rect);

    auto playback_button_is_hovered = mouse_position.has_value() && playback_button_hover_rect.contains(*mouse_position);
    auto playback_button_color = control_button_color(playback_button_is_hovered);

    if (video_element.paused()) {
        Array<Gfx::IntPoint, 3> play_button_coordinates { {
            { 0, 0 },
            { static_cast<int>(playback_button_size), static_cast<int>(playback_button_size) / 2 },
            { 0, static_cast<int>(playback_button_size) },
        } };

        context.painter().draw_triangle(playback_button_location.to_type<int>(), play_button_coordinates, playback_button_color);
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

DevicePixelRect VideoPaintable::paint_control_bar_timeline(PaintContext& context, HTML::HTMLVideoElement const& video_element, DevicePixelRect control_box_rect, Optional<DevicePixelPoint> const& mouse_position) const
{
    auto maximum_timeline_button_size = context.rounded_device_pixels(16);

    auto timeline_rect = control_box_rect;
    timeline_rect.set_width(min(control_box_rect.width() * 6 / 10, timeline_rect.width()));

    auto playback_percentage = video_element.current_time() / video_element.duration();
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

DevicePixelRect VideoPaintable::paint_control_bar_timestamp(PaintContext& context, HTML::HTMLVideoElement const& video_element, DevicePixelRect control_box_rect) const
{
    auto current_time = human_readable_digital_time(round(video_element.current_time()));
    auto duration = human_readable_digital_time(round(video_element.duration()));
    auto timestamp = String::formatted("{} / {}", current_time, duration).release_value_but_fixme_should_propagate_errors();

    auto timestamp_size = static_cast<DevicePixels::Type>(ceilf(context.painter().font().width(timestamp)));
    if (timestamp_size > control_box_rect.width())
        return control_box_rect;

    auto timestamp_rect = control_box_rect;
    timestamp_rect.set_width(timestamp_size);
    context.painter().draw_text(timestamp_rect.to_type<int>(), timestamp, Gfx::TextAlignment::CenterLeft, Color::White);

    control_box_rect.take_from_left(timestamp_rect.width());
    return control_box_rect;
}

void VideoPaintable::paint_placeholder_video_controls(PaintContext& context, DevicePixelRect video_rect, Optional<DevicePixelPoint> const& mouse_position) const
{
    auto maximum_control_box_size = context.rounded_device_pixels(100);
    auto maximum_playback_button_size = context.rounded_device_pixels(40);

    auto center = video_rect.center();

    auto control_box_size = min(maximum_control_box_size, min(video_rect.width(), video_rect.height()) * 4 / 5);
    auto control_box_offset_x = control_box_size / 2;
    auto control_box_offset_y = control_box_size / 2;

    auto control_box_location = center.translated(-control_box_offset_x, -control_box_offset_y);
    DevicePixelRect control_box_rect { control_box_location, { control_box_size, control_box_size } };

    auto playback_button_size = min(maximum_playback_button_size, min(video_rect.width(), video_rect.height()) * 2 / 5);
    auto playback_button_offset_x = playback_button_size / 2;
    auto playback_button_offset_y = playback_button_size / 2;

    // We want to center the play button on its center of mass, which is not the midpoint of its vertices.
    // To do so, reduce its desired x offset by a factor of tan(30 degrees) / 2 (about 0.288685).
    playback_button_offset_x -= 0.288685f * static_cast<float>(static_cast<DevicePixels::Type>(playback_button_offset_x));

    auto playback_button_location = center.translated(-playback_button_offset_x, -playback_button_offset_y);

    Array<Gfx::IntPoint, 3> play_button_coordinates { {
        { 0, 0 },
        { static_cast<int>(playback_button_size), static_cast<int>(playback_button_size) / 2 },
        { 0, static_cast<int>(playback_button_size) },
    } };

    auto playback_button_is_hovered = mouse_position.has_value() && control_box_rect.contains(*mouse_position);
    auto playback_button_color = control_button_color(playback_button_is_hovered);

    Gfx::AntiAliasingPainter painter { context.painter() };
    painter.fill_ellipse(control_box_rect.to_type<int>(), control_box_color);
    context.painter().draw_triangle(playback_button_location.to_type<int>(), play_button_coordinates, playback_button_color);
}

VideoPaintable::DispatchEventOfSameName VideoPaintable::handle_mouseup(Badge<EventHandler>, CSSPixelPoint position, unsigned button, unsigned)
{
    if (button != GUI::MouseButton::Primary)
        return DispatchEventOfSameName::No;

    auto& video_element = layout_box().dom_node();
    auto const& cached_layout_boxes = video_element.cached_layout_boxes({});

    // FIXME: This runs from outside the context of any user script, so we do not have a running execution
    //        context. This pushes one to allow the promise creation hook to run.
    auto& environment_settings = document().relevant_settings_object();
    environment_settings.prepare_to_run_script();

    ScopeGuard guard { [&] { environment_settings.clean_up_after_running_script(); } };

    auto toggle_playback = [&]() -> WebIDL::ExceptionOr<void> {
        if (video_element.paused())
            TRY(video_element.play());
        else
            TRY(video_element.pause());
        return {};
    };

    if (cached_layout_boxes.control_box_rect.has_value() && cached_layout_boxes.control_box_rect->contains(position)) {
        if (cached_layout_boxes.playback_button_rect.has_value() && cached_layout_boxes.playback_button_rect->contains(position)) {
            toggle_playback().release_value_but_fixme_should_propagate_errors();
            return DispatchEventOfSameName::Yes;
        }

        return DispatchEventOfSameName::No;
    }

    toggle_playback().release_value_but_fixme_should_propagate_errors();
    return DispatchEventOfSameName::Yes;
}

VideoPaintable::DispatchEventOfSameName VideoPaintable::handle_mousemove(Badge<EventHandler>, CSSPixelPoint position, unsigned, unsigned)
{
    auto& video_element = layout_box().dom_node();

    if (absolute_rect().contains(position)) {
        video_element.set_layout_mouse_position({}, position);
        return DispatchEventOfSameName::Yes;
    }

    video_element.set_layout_mouse_position({}, {});
    return DispatchEventOfSameName::No;
}

}
