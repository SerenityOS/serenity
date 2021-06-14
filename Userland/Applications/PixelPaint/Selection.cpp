/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Selection.h"
#include "ImageEditor.h"
#include <LibGfx/Painter.h>

namespace PixelPaint {

void Selection::paint(Gfx::Painter& painter, ImageEditor const& editor)
{
    painter.draw_rect(editor.image_rect_to_editor_rect(m_rect).to_type<int>(), Color::Magenta);
}

}
