/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
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
    ScopedCornerRadiusClip corner_clip { context, context.painter(), video_rect, normalized_border_radii_data(ShrinkRadiiForBorders::Yes) };

    Optional<DevicePixelPoint> mouse_position;
    if (m_mouse_position.has_value())
        mouse_position = context.rounded_device_point(*m_mouse_position);

    auto const& video_element = layout_box().dom_node();
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
    auto maximum_playback_button_size = context.rounded_device_pixels(15);
    auto maximum_playback_button_offset_x = context.rounded_device_pixels(15);

    auto is_hovered = document().hovered_node() == &video_element;
    auto is_paused = video_element.paused();

    if (!is_hovered && !is_paused)
        return;

    auto control_box_rect = video_rect;
    if (control_box_rect.height() > maximum_control_box_size)
        control_box_rect.take_from_top(control_box_rect.height() - maximum_control_box_size);

    context.painter().fill_rect(control_box_rect.to_type<int>(), control_box_color.with_alpha(0xd0));

    auto playback_button_size = min(maximum_playback_button_size, control_box_rect.height() / 2);
    auto playback_button_offset_x = min(maximum_playback_button_offset_x, control_box_rect.width());
    auto playback_button_offset_y = (control_box_rect.height() - playback_button_size) / 2;

    auto playback_button_location = control_box_rect.top_left().translated(playback_button_offset_x, playback_button_offset_y);

    auto playback_button_hover_rect = DevicePixelRect {
        control_box_rect.top_left(),
        { playback_button_size + playback_button_offset_x * 2, control_box_rect.height() }
    };

    auto playback_button_is_hovered = mouse_position.has_value() && playback_button_hover_rect.contains(*mouse_position);
    auto playback_button_color = control_button_color(playback_button_is_hovered);

    if (is_paused) {
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

VideoPaintable::DispatchEventOfSameName VideoPaintable::handle_mouseup(Badge<EventHandler>, CSSPixelPoint, unsigned button, unsigned)
{
    if (button != GUI::MouseButton::Primary)
        return DispatchEventOfSameName::No;

    auto& video_element = layout_box().dom_node();

    // FIXME: This runs from outside the context of any user script, so we do not have a running execution
    //        context. This pushes one to allow the promise creation hook to run.
    auto& environment_settings = document().relevant_settings_object();
    environment_settings.prepare_to_run_script();

    if (video_element.paused())
        video_element.play().release_value_but_fixme_should_propagate_errors();
    else
        video_element.pause().release_value_but_fixme_should_propagate_errors();

    environment_settings.clean_up_after_running_script();
    return DispatchEventOfSameName::Yes;
}

VideoPaintable::DispatchEventOfSameName VideoPaintable::handle_mousemove(Badge<EventHandler>, CSSPixelPoint position, unsigned, unsigned)
{
    if (absolute_rect().contains(position)) {
        m_mouse_position = position;
        return DispatchEventOfSameName::Yes;
    }

    m_mouse_position.clear();
    return DispatchEventOfSameName::No;
}

}
