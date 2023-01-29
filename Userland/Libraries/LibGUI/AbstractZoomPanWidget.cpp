/*
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AbstractZoomPanWidget.h"

namespace GUI {

constexpr float wheel_zoom_factor = 8.0f;

void AbstractZoomPanWidget::set_scale(float new_scale)
{
    if (m_original_rect.is_empty())
        return;

    m_scale = clamp(new_scale, m_min_scale, m_max_scale);
    m_content_rect.set_size({
        m_original_rect.width() * m_scale,
        m_original_rect.height() * m_scale,
    });

    if (on_scale_change)
        on_scale_change(m_scale);

    relayout();
}

void AbstractZoomPanWidget::scale_by(float delta)
{
    float new_scale = m_scale * AK::exp2(delta);
    set_scale(new_scale);
}

void AbstractZoomPanWidget::scale_centered(float new_scale, Gfx::IntPoint center)
{
    if (m_original_rect.is_empty())
        return;

    new_scale = clamp(new_scale, m_min_scale, m_max_scale);
    if (new_scale == m_scale)
        return;

    Gfx::FloatPoint focus_point {
        center.x() - width() / 2.0f,
        center.y() - height() / 2.0f,
    };
    m_origin = (m_origin + focus_point) * (new_scale / m_scale) - focus_point;
    set_scale(new_scale);
}

void AbstractZoomPanWidget::start_panning(Gfx::IntPoint position)
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

void AbstractZoomPanWidget::pan_to(Gfx::IntPoint position)
{
    // NOTE: `position` here (and `m_pan_mouse_pos`) are both in frame coordinates, not
    // content coordinates, by design. The derived class should not have to keep track of
    // the (zoomed) content coordinates itself, but just pass along the mouse position.
    auto delta = position - m_pan_mouse_pos;
    m_origin = m_pan_start.translated(-delta.x(), -delta.y());
    relayout();
}

Gfx::FloatPoint AbstractZoomPanWidget::frame_to_content_position(Gfx::IntPoint frame_position) const
{
    return {
        (static_cast<float>(frame_position.x()) - m_content_rect.x()) / m_scale,
        (static_cast<float>(frame_position.y()) - m_content_rect.y()) / m_scale,
    };
}

Gfx::FloatRect AbstractZoomPanWidget::frame_to_content_rect(Gfx::IntRect const& frame_rect) const
{
    Gfx::FloatRect content_rect;
    content_rect.set_location(frame_to_content_position(frame_rect.location()));
    content_rect.set_size({
        frame_rect.width() / m_scale,
        frame_rect.height() / m_scale,
    });
    return content_rect;
}

Gfx::FloatPoint AbstractZoomPanWidget::content_to_frame_position(Gfx::IntPoint content_position) const
{
    return {
        m_content_rect.x() + content_position.x() * m_scale,
        m_content_rect.y() + content_position.y() * m_scale,
    };
}

Gfx::FloatRect AbstractZoomPanWidget::content_to_frame_rect(Gfx::IntRect const& content_rect) const
{
    Gfx::FloatRect frame_rect;
    frame_rect.set_location(content_to_frame_position(content_rect.location()));
    frame_rect.set_size({
        content_rect.width() * m_scale,
        content_rect.height() * m_scale,
    });
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
    if (m_original_rect.is_empty())
        return;

    m_content_rect.set_location({
        (width() / 2) - (m_content_rect.width() / 2) - m_origin.x(),
        (height() / 2) - (m_content_rect.height() / 2) - m_origin.y(),
    });

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

void AbstractZoomPanWidget::fit_content_to_rect(Gfx::IntRect const& viewport_rect, FitType type)
{
    float const border_ratio = 0.95f;
    auto image_size = m_original_rect.size();
    auto height_ratio = floorf(border_ratio * viewport_rect.height()) / image_size.height();
    auto width_ratio = floorf(border_ratio * viewport_rect.width()) / image_size.width();

    float new_scale = 1.0f;
    switch (type) {
    case FitType::Width:
        new_scale = width_ratio;
        break;
    case FitType::Height:
        new_scale = height_ratio;
        break;
    case FitType::Both:
        new_scale = min(height_ratio, width_ratio);
        break;
    }

    auto const& offset = rect().center() - viewport_rect.center();
    set_origin({ offset.x(), offset.y() });
    set_scale(new_scale);
}

}
