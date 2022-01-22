/*
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AbstractZoomPanWidget.h"

namespace GUI {

constexpr float wheel_zoom_factor = 8.0f;

void AbstractZoomPanWidget::set_scale(float new_scale)
{
    if (m_original_rect.is_null())
        return;

    m_scale = clamp(new_scale, m_min_scale, m_max_scale);
    Gfx::IntSize new_size;
    new_size.set_width(m_original_rect.width() * m_scale);
    new_size.set_height(m_original_rect.height() * m_scale);
    m_content_rect.set_size(new_size);

    if (on_scale_change)
        on_scale_change(m_scale);

    relayout();
}

void AbstractZoomPanWidget::scale_by(float delta)
{
    float new_scale = m_scale * AK::exp2(delta);
    set_scale(new_scale);
}

void AbstractZoomPanWidget::scale_centered(float new_scale, Gfx::IntPoint const& center)
{
    if (m_original_rect.is_null())
        return;

    new_scale = clamp(new_scale, m_min_scale, m_max_scale);
    if (new_scale == m_scale)
        return;

    Gfx::FloatPoint focus_point {
        center.x() - width() / 2.0f,
        center.y() - height() / 2.0f
    };
    m_origin = (m_origin + focus_point) * (new_scale / m_scale) - focus_point;
    set_scale(new_scale);
}

void AbstractZoomPanWidget::start_panning(Gfx::IntPoint const& position)
{
    m_saved_cursor = override_cursor();
    set_override_cursor(Gfx::StandardCursor::Drag);
    m_pan_start = m_origin;
    m_pan_mouse_pos = position;
    m_is_panning = true;
}

void AbstractZoomPanWidget::stop_panning()
{
    m_is_panning = false;
    set_override_cursor(m_saved_cursor);
}

void AbstractZoomPanWidget::pan_to(Gfx::IntPoint const& position)
{
    // NOTE: `position` here (and `m_pan_mouse_pos`) are both in frame coordinates, not
    // content coordinates, by design. The derived class should not have to keep track of
    // the (zoomed) content coordinates itself, but just pass along the mouse position.
    auto delta = position - m_pan_mouse_pos;
    m_origin = m_pan_start.translated(-delta.x(), -delta.y());
    relayout();
}

Gfx::FloatPoint AbstractZoomPanWidget::frame_to_content_position(Gfx::IntPoint const& frame_position) const
{
    Gfx::FloatPoint content_position;
    content_position.set_x(((float)frame_position.x() - (float)m_content_rect.x()) / m_scale);
    content_position.set_y(((float)frame_position.y() - (float)m_content_rect.y()) / m_scale);
    return content_position;
}

Gfx::FloatRect AbstractZoomPanWidget::frame_to_content_rect(Gfx::IntRect const& frame_rect) const
{
    Gfx::FloatRect content_rect;
    content_rect.set_location(frame_to_content_position(frame_rect.location()));
    content_rect.set_width((float)frame_rect.width() / m_scale);
    content_rect.set_height((float)frame_rect.height() / m_scale);
    return content_rect;
}

Gfx::FloatPoint AbstractZoomPanWidget::content_to_frame_position(Gfx::IntPoint const& content_position) const
{
    Gfx::FloatPoint frame_position;
    frame_position.set_x(m_content_rect.x() + ((float)content_position.x() * m_scale));
    frame_position.set_y(m_content_rect.y() + ((float)content_position.y() * m_scale));
    return frame_position;
}

Gfx::FloatRect AbstractZoomPanWidget::content_to_frame_rect(Gfx::IntRect const& content_rect) const
{
    Gfx::FloatRect frame_rect;
    frame_rect.set_location(content_to_frame_position(content_rect.location()));
    frame_rect.set_width((float)content_rect.width() * m_scale);
    frame_rect.set_height((float)content_rect.height() * m_scale);
    return frame_rect;
}

void AbstractZoomPanWidget::mousewheel_event(GUI::MouseEvent& event)
{
    float new_scale = scale() / AK::exp2(event.wheel_delta_y() / wheel_zoom_factor);
    scale_centered(new_scale, event.position());
}

void AbstractZoomPanWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (!m_is_panning && event.button() == GUI::MouseButton::Middle) {
        start_panning(event.position());
        event.accept();
        return;
    }
}

void AbstractZoomPanWidget::resize_event(GUI::ResizeEvent& event)
{
    relayout();
    GUI::Widget::resize_event(event);
}

void AbstractZoomPanWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (!m_is_panning)
        return;
    pan_to(event.position());
    event.accept();
}

void AbstractZoomPanWidget::mouseup_event(GUI::MouseEvent& event)
{
    if (m_is_panning && event.button() == GUI::MouseButton::Middle) {
        stop_panning();
        event.accept();
        return;
    }
}

void AbstractZoomPanWidget::relayout()
{
    if (m_original_rect.is_null())
        return;

    Gfx::IntSize new_size = m_content_rect.size();

    Gfx::IntPoint new_location;
    new_location.set_x((width() / 2) - (new_size.width() / 2) - m_origin.x());
    new_location.set_y((height() / 2) - (new_size.height() / 2) - m_origin.y());
    m_content_rect.set_location(new_location);

    handle_relayout(m_content_rect);
}

void AbstractZoomPanWidget::reset_view()
{
    m_origin = { 0, 0 };
    set_scale(1.0f);
}

void AbstractZoomPanWidget::set_content_rect(Gfx::IntRect const& content_rect)
{
    m_content_rect = enclosing_int_rect(content_to_frame_rect(content_rect));
    update();
}

void AbstractZoomPanWidget::set_scale_bounds(float min_scale, float max_scale)
{
    m_min_scale = min_scale;
    m_max_scale = max_scale;
}

}
