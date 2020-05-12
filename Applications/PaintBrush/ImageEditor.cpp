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

#include "ImageEditor.h"
#include "Image.h"
#include "Layer.h"
#include "LayerModel.h"
#include "Tool.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

namespace PaintBrush {

ImageEditor::ImageEditor()
{
}

void ImageEditor::set_image(RefPtr<Image> image)
{
    m_image = move(image);
    update();
}

void ImageEditor::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect_with_checkerboard(rect(), { 8, 8 }, palette().base().darkened(0.9), palette().base());

    if (m_image) {
        m_image->paint_into(painter, m_image->rect(), m_image->rect());
    }

    if (m_active_layer) {
        painter.draw_rect(m_active_layer->rect().inflated(2, 2), Color::Black);
    }
}

static GUI::MouseEvent event_adjusted_for_layer(const GUI::MouseEvent& original_event, const Layer& layer)
{
    auto position_in_active_layer_coordinates = original_event.position().translated(-layer.location());
    dbg() << "adjusted: " << position_in_active_layer_coordinates;
    return {
        static_cast<GUI::Event::Type>(original_event.type()),
        position_in_active_layer_coordinates, original_event.buttons(),
        original_event.button(),
        original_event.modifiers(),
        original_event.wheel_delta()
    };
}

void ImageEditor::mousedown_event(GUI::MouseEvent& event)
{
    if (!m_active_layer || !m_active_tool)
        return;
    auto layer_event = event_adjusted_for_layer(event, *m_active_layer);
    m_active_tool->on_mousedown(*m_active_layer, layer_event);
}

void ImageEditor::mousemove_event(GUI::MouseEvent& event)
{
    if (!m_active_layer || !m_active_tool)
        return;
    auto layer_event = event_adjusted_for_layer(event, *m_active_layer);
    m_active_tool->on_mousemove(*m_active_layer, layer_event);
}

void ImageEditor::mouseup_event(GUI::MouseEvent& event)
{
    if (!m_active_layer || !m_active_tool)
        return;
    auto layer_event = event_adjusted_for_layer(event, *m_active_layer);
    m_active_tool->on_mouseup(*m_active_layer, layer_event);
}

void ImageEditor::set_active_layer(Layer* layer)
{
    if (m_active_layer == layer)
        return;
    m_active_layer = layer;
    update();
}

void ImageEditor::set_active_tool(Tool* tool)
{
    if (m_active_tool == tool)
        return;

    if (m_active_tool)
        m_active_tool->clear();

    m_active_tool = tool;

    if (m_active_tool)
        m_active_tool->setup(*this);
}

}
