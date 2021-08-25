/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MoveTool.h"
#include "Image.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

namespace PixelPaint {

MoveTool::MoveTool()
{
}

MoveTool::~MoveTool()
{
}

void MoveTool::on_mousedown(Layer& layer, MouseEvent& event)
{
    auto& layer_event = event.layer_event();
    auto& image_event = event.image_event();
    if (layer_event.button() != GUI::MouseButton::Left)
        return;
    if (!layer.rect().contains(layer_event.position()))
        return;
    m_layer_being_moved = layer;
    m_event_origin = image_event.position();
    m_layer_origin = layer.location();
}

void MoveTool::on_mousemove(Layer&, MouseEvent& event)
{
    auto& image_event = event.image_event();
    if (!m_layer_being_moved)
        return;
    auto delta = image_event.position() - m_event_origin;
    m_layer_being_moved->set_location(m_layer_origin.translated(delta));
    m_editor->layers_did_change();
}

void MoveTool::on_mouseup(Layer&, MouseEvent& event)
{
    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Left)
        return;
    m_layer_being_moved = nullptr;
    m_editor->did_complete_action();
}

void MoveTool::on_keydown(GUI::KeyEvent& event)
{
    if (event.modifiers() != 0)
        return;

    auto* layer = m_editor->active_layer();
    if (!layer)
        return;

    auto new_location = layer->location();

    switch (event.key()) {
    case Key_Up:
        new_location.translate_by(0, -1);
        break;
    case Key_Down:
        new_location.translate_by(0, 1);
        break;
    case Key_Left:
        new_location.translate_by(-1, 0);
        break;
    case Key_Right:
        new_location.translate_by(1, 0);
        break;
    default:
        return;
    }

    layer->set_location(new_location);
    m_editor->layers_did_change();
}

}
