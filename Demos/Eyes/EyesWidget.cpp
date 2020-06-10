/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "EyesWidget.h"
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Palette.h>
#include <math.h>

EyesWidget::~EyesWidget()
{
}

void EyesWidget::track_cursor_globally()
{
    ASSERT(window());
    auto window_id = window()->window_id();
    ASSERT(window_id >= 0);

    set_global_cursor_tracking(true);
    GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetGlobalCursorTracking>(window_id, true);
}

void EyesWidget::mousemove_event(GUI::MouseEvent& event)
{
    m_mouse_position = event.position();
    update();
}

void EyesWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);

    painter.clear_rect(event.rect(), Gfx::Color());

    for (int i = 0; i < m_num_eyes; i++)
        render_eyeball(i, painter);
}

void EyesWidget::render_eyeball(int index, GUI::Painter& painter) const
{
    auto eye_width = width() / m_num_eyes;
    Gfx::IntRect bounds { index * eye_width, 0, eye_width, height() };
    auto width_thickness = max(int(eye_width / 5.5), 1);
    auto height_thickness = max(int(height() / 5.5), 1);

    bounds.shrink(int(eye_width / 12.5), 0);
    painter.fill_ellipse(bounds, palette().base_text());
    bounds.shrink(width_thickness, height_thickness);
    painter.fill_ellipse(bounds, palette().base());

    Gfx::IntPoint pupil_center = this->pupil_center(bounds);
    Gfx::IntSize pupil_size {
        bounds.width() / 5,
        bounds.height() / 5
    };
    Gfx::IntRect pupil {
        pupil_center.x() - pupil_size.width() / 2,
        pupil_center.y() - pupil_size.height() / 2,
        pupil_size.width(),
        pupil_size.height()
    };

    painter.fill_ellipse(pupil, palette().base_text());
}

Gfx::IntPoint EyesWidget::pupil_center(Gfx::IntRect& eyeball_bounds) const
{
    auto mouse_vector = m_mouse_position - eyeball_bounds.center();
    double dx = mouse_vector.x();
    double dy = mouse_vector.y();
    double mouse_distance = sqrt(dx * dx + dy * dy);

    if (mouse_distance == 0.0)
        return eyeball_bounds.center();

    double width_squared = eyeball_bounds.width() * eyeball_bounds.width();
    double height_squared = eyeball_bounds.height() * eyeball_bounds.height();

    double max_distance_along_this_direction;

    if (dx != 0 && abs(dx) >= abs(dy)) {
        double slope = dy / dx;
        double slope_squared = slope * slope;
        max_distance_along_this_direction = 0.25 * sqrt(
            (slope_squared + 1) /
            (1 / width_squared + slope_squared / height_squared)
        );
    } else if (dy != 0 && abs(dy) >= abs(dx)) {
        double slope = dx / dy;
        double slope_squared = slope * slope;
        max_distance_along_this_direction = 0.25 * sqrt(
            (slope_squared + 1) /
            (slope_squared / width_squared + 1 / height_squared)
        );
    } else {
        ASSERT_NOT_REACHED();
    }

    double scale = min(1.0, max_distance_along_this_direction / mouse_distance);

    return {
        eyeball_bounds.center().x() + int(dx * scale),
        eyeball_bounds.center().y() + int(dy * scale)
    };
}
