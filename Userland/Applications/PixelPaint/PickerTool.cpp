/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PickerTool.h"
#include "ImageEditor.h"
#include "Layer.h"

namespace PixelPaint {

PickerTool::PickerTool()
{
}

PickerTool::~PickerTool()
{
}

void PickerTool::on_mousedown(Layer& layer, MouseEvent& event)
{
    auto& layer_event = event.layer_event();
    if (!layer.rect().contains(layer_event.position()))
        return;
    auto color = layer.bitmap().get_pixel(layer_event.position());
    if (layer_event.button() == GUI::MouseButton::Left)
        m_editor->set_primary_color(color);
    else if (layer_event.button() == GUI::MouseButton::Right)
        m_editor->set_secondary_color(color);
}

}
