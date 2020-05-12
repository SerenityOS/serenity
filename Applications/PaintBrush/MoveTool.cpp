/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include "MoveTool.h"
#include "ImageEditor.h"
#include "Layer.h"
#include "PaintableWidget.h"
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

namespace PaintBrush {

MoveTool::MoveTool()
{
}

MoveTool::~MoveTool()
{
}

void MoveTool::on_mousedown(Layer& layer, GUI::MouseEvent& event, GUI::MouseEvent& original_event)
{
    if (event.button() != GUI::MouseButton::Left)
        return;
    if (!layer.rect().contains(event.position()))
        return;
    m_layer_being_moved = layer;
    m_event_origin = original_event.position();
    m_layer_origin = layer.location();
    m_editor->window()->set_override_cursor(GUI::StandardCursor::Move);
}

void MoveTool::on_mousemove(Layer&, GUI::MouseEvent&, GUI::MouseEvent& original_event)
{
    if (!m_layer_being_moved)
        return;
    auto delta = original_event.position() - m_event_origin;
    m_layer_being_moved->set_location(m_layer_origin.translated(delta));
    m_editor->update();
}

void MoveTool::on_mouseup(Layer&, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (event.button() != GUI::MouseButton::Left)
        return;
    m_layer_being_moved = nullptr;
    m_editor->window()->set_override_cursor(GUI::StandardCursor::None);
}

}
