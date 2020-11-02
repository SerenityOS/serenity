/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "CursorTool.h"
#include "FormEditorWidget.h"
#include "FormWidget.h"
#include "WidgetTreeModel.h"
#include <AK/LogStream.h>
#include <LibGfx/Palette.h>

//#define DEBUG_CURSOR_TOOL

namespace HackStudio {

void CursorTool::on_mousedown(GUI::MouseEvent& event)
{
#ifdef DEBUG_CURSOR_TOOL
    dbgln("CursorTool::on_mousedown");
#endif

    if (m_resize_direction == Direction::None) {
        auto grabber = m_editor.form_widget().grabber_at(event.position());
        if (grabber != Direction::None) {
            m_current_event_origin = event.position();
            m_resize_direction = grabber;
            return;
        }
    }

    auto& form_widget = m_editor.form_widget();

    auto result = form_widget.hit_test(event.position(), GUI::Widget::ShouldRespectGreediness::No);
    if (event.button() == GUI::MouseButton::Left) {
        if (result.widget && result.widget != &form_widget) {
            if (event.modifiers() & Mod_Ctrl) {
                m_editor.selection().toggle(*result.widget);
            } else if (!event.modifiers()) {
                if (!m_editor.selection().contains(*result.widget)) {
#ifdef DEBUG_CURSOR_TOOL
                    dbg() << "Selection didn't contain " << *result.widget << ", making it the only selected one";
#endif
                    m_editor.selection().set(*result.widget);
                }

                m_current_event_origin = event.position();
            }
        } else {
            m_editor.selection().clear();
            m_rubber_banding = true;
            m_rubber_band_origin = event.position();
            m_rubber_band_position = event.position();
        }
    }
}

void CursorTool::on_mouseup(GUI::MouseEvent& event)
{
#ifdef DEBUG_CURSOR_TOOL
    dbgln("CursorTool::on_mouseup");
#endif
    if (event.button() == GUI::MouseButton::Left) {
        auto& form_widget = m_editor.form_widget();
        auto result = form_widget.hit_test(event.position(), GUI::Widget::ShouldRespectGreediness::No);
        if (!m_dragging && !(event.modifiers() & Mod_Ctrl)) {
            if (result.widget && result.widget != &form_widget) {
                m_editor.selection().set(*result.widget);
            }
        }
        m_dragging = false;
        m_rubber_banding = false;
    }

    m_editor.update();
    m_resize_direction = Direction::None;
    m_editor.form_widget().set_override_cursor(Gfx::StandardCursor::None);
}

void CursorTool::on_mousemove(GUI::MouseEvent& event)
{
#ifdef DEBUG_CURSOR_TOOL
    dbgln("CursorTool::on_mousemove");
#endif
    if (event.buttons() & GUI::MouseButton::Left) {
        if (m_resize_direction != Direction::None) {
            resize_widgets(event);
            m_current_event_origin = event.position();
            return;
        }

        if (m_rubber_banding) {
            set_rubber_band_position(event.position());
            return;
        }

        m_dragging = true;
        m_editor.update();
        m_editor.form_widget().set_override_cursor(Gfx::StandardCursor::Drag);

        auto movement_delta = event.position() - m_current_event_origin;
        m_current_event_origin = event.position();
        m_editor.selection().for_each([&](auto& widget) {
            auto new_rect = widget.relative_rect().translated(movement_delta);
            widget.set_relative_rect(new_rect);
            return IterationDecision::Continue;
        });

        m_editor.form_widget().update();
    } else {
        auto grabber = m_editor.form_widget().grabber_at(event.position());
        set_cursor_type_from_grabber(grabber);
    }
}

void CursorTool::on_keydown(GUI::KeyEvent& event)
{
#ifdef DEBUG_CURSOR_TOOL
    dbgln("CursorTool::on_keydown");
#endif

    auto move_selected_widgets_by = [this](int x, int y) {
        m_editor.selection().for_each([&](auto& widget) {
            widget.move_by(x, y);
            return IterationDecision::Continue;
        });
    };

    if (event.modifiers() == 0) {
        auto grid_size = m_editor.form_widget().grid_size();
        switch (event.key()) {
        case Key_Down:
            move_selected_widgets_by(0, grid_size);
            break;
        case Key_Up:
            move_selected_widgets_by(0, -grid_size);
            break;
        case Key_Left:
            move_selected_widgets_by(-grid_size, 0);
            break;
        case Key_Right:
            move_selected_widgets_by(grid_size, 0);
            break;
        default:
            break;
        }
    }
}

void CursorTool::set_rubber_band_position(const Gfx::IntPoint& position)
{
    if (m_rubber_band_position == position)
        return;

    m_rubber_band_position = position;
    m_editor.selection().clear();

    auto rubber_band_rect = this->rubber_band_rect();
    m_editor.form_widget().for_each_child_widget([&](auto& child) {
        if (child.relative_rect().intersects(rubber_band_rect))
            m_editor.selection().add(child);
        return IterationDecision::Continue;
    });
}

Gfx::IntRect CursorTool::rubber_band_rect() const
{
    if (!m_rubber_banding)
        return {};
    return Gfx::IntRect::from_two_points(m_rubber_band_origin, m_rubber_band_position);
}

void CursorTool::on_second_paint(GUI::Painter& painter, GUI::PaintEvent&)
{
    if (!m_rubber_banding)
        return;
    auto rect = rubber_band_rect();
    painter.fill_rect(rect, m_editor.palette().rubber_band_fill());
    painter.draw_rect(rect, m_editor.palette().rubber_band_border());
}

void CursorTool::resize_widgets(GUI::MouseEvent& event)
{
    int diff_x = event.x() - m_current_event_origin.x();
    int diff_y = event.y() - m_current_event_origin.y();

    int change_x = 0;
    int change_y = 0;
    int change_w = 0;
    int change_h = 0;

    switch (m_resize_direction) {
    case Direction::DownRight:
        change_w = diff_x;
        change_h = diff_y;
        break;
    case Direction::Right:
        change_w = diff_x;
        break;
    case Direction::UpRight:
        change_w = diff_x;
        change_y = diff_y;
        change_h = -diff_y;
        break;
    case Direction::Up:
        change_y = diff_y;
        change_h = -diff_y;
        break;
    case Direction::UpLeft:
        change_x = diff_x;
        change_w = -diff_x;
        change_y = diff_y;
        change_h = -diff_y;
        break;
    case Direction::Left:
        change_x = diff_x;
        change_w = -diff_x;
        break;
    case Direction::DownLeft:
        change_x = diff_x;
        change_w = -diff_x;
        change_h = diff_y;
        break;
    case Direction::Down:
        change_h = diff_y;
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    m_editor.selection().for_each([&](GUI::Widget& widget) {
        Gfx::IntSize minimum_size { 10, 10 };
        widget.set_x(widget.x() + change_x);
        widget.set_y(widget.y() + change_y);
        widget.set_width(max(minimum_size.width(), widget.width() + change_w));
        widget.set_height(max(minimum_size.height(), widget.height() + change_h));
        return IterationDecision::Continue;
    });
    m_editor.form_widget().update();
}

void CursorTool::set_cursor_type_from_grabber(Direction grabber)
{
    if (grabber == m_mouse_direction_type)
        return;

    switch (grabber) {
    case Direction::Up:
    case Direction::Down:
        m_editor.form_widget().set_override_cursor(Gfx::StandardCursor::ResizeVertical);
        break;
    case Direction::Left:
    case Direction::Right:
        m_editor.form_widget().set_override_cursor(Gfx::StandardCursor::ResizeHorizontal);
        break;
    case Direction::UpLeft:
    case Direction::DownRight:
        m_editor.form_widget().set_override_cursor(Gfx::StandardCursor::ResizeDiagonalTLBR);
        break;
    case Direction::UpRight:
    case Direction::DownLeft:
        m_editor.form_widget().set_override_cursor(Gfx::StandardCursor::ResizeDiagonalBLTR);
        break;
    case Direction::None:
        m_editor.form_widget().set_override_cursor(Gfx::StandardCursor::None);
        break;
    }

    m_mouse_direction_type = grabber;
}

}
