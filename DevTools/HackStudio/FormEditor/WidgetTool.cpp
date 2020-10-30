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

#include "WidgetTool.h"
#include "FormEditorWidget.h"
#include "FormWidget.h"
#include <AK/LogStream.h>
#include <LibGfx/StandardCursor.h>

namespace HackStudio {

void WidgetTool::on_mousedown(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Left) {
        m_down_event_origin = event.position();
        m_editor.selection().for_each([this](auto& widget) {
            widget.set_relative_rect({ m_down_event_origin.x(), m_down_event_origin.y(), 0, 0 });
            return IterationDecision::Break;
        });
    }
}

void WidgetTool::on_mousemove(GUI::MouseEvent& event)
{
    m_editor.form_widget().set_override_cursor(Gfx::StandardCursor::Move);

    if (event.buttons() & GUI::MouseButton::Left) {
        m_editor.update();
        auto delta = event.position() - m_down_event_origin;
        m_editor.selection().for_each([&delta](auto& widget) {
            widget.set_width(delta.x());
            widget.set_height(delta.y());
            return IterationDecision::Break;
        });
    }
}

void WidgetTool::on_mouseup(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Left) {
        m_down_event_origin = {};
    }

    m_editor.selection().for_each([](auto& widget) {
        // When there is no MOUSE_MOVE event, set the default width and height to 30
        if (widget.width() < 1 && widget.height() < 1) {
            widget.set_width(30);
            widget.set_height(30);
        }
        return IterationDecision::Break;
    });
    m_editor.form_widget().set_override_cursor(Gfx::StandardCursor::None);

    m_editor.activate_cursor_tool();
}

}
