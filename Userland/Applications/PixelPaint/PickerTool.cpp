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

void PickerTool::on_mousedown(Layer& layer, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (!layer.rect().contains(event.position()))
        return;
    auto color = layer.bitmap().get_pixel(event.position());
    if (event.button() == GUI::MouseButton::Left)
        m_editor->set_primary_color(color);
    else if (event.button() == GUI::MouseButton::Right)
        m_editor->set_secondary_color(color);
}

}
