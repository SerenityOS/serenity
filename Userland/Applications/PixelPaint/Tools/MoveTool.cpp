/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MoveTool.h"
#include "../Image.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Window.h>

namespace PixelPaint {

void MoveTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (event.image_event().button() == GUI::MouseButton::Secondary) {
        m_editor->start_panning(event.raw_event().position());
        return;
    }

    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    auto& image_event = event.image_event();
    if (layer_event.button() != GUI::MouseButton::Primary)
        return;
    if (!layer->rect().contains(layer_event.position()))
        return;
    m_layer_being_moved = *layer;
    m_event_origin = image_event.position();
    m_layer_origin = layer->location();
}

void MoveTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (m_editor->is_panning()) {
        m_editor->pan_to(event.raw_event().position());
        return;
    }

    if (!layer)
        return;

    auto& image_event = event.image_event();
    if (!m_layer_being_moved)
        return;
    auto delta = image_event.position() - m_event_origin;
    m_layer_being_moved->set_location(m_layer_origin.translated(delta));
    m_editor->layers_did_change();
}

void MoveTool::on_mouseup(Layer* layer, MouseEvent& event)
{
    if (event.image_event().button() == GUI::MouseButton::Secondary) {
        m_editor->stop_panning();
        m_editor->set_override_cursor(cursor());
        return;
    }

    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Primary)
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
