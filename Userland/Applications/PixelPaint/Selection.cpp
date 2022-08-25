/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Selection.h"
#include "ImageEditor.h"
#include <LibGfx/Painter.h>

namespace PixelPaint {

Selection::Selection(ImageEditor& editor)
    : m_editor(editor)
{
}

void Selection::clear()
{
    m_mask = {};
    m_editor.update();
}

void Selection::merge(Mask const& mask, MergeMode mode)
{
    switch (mode) {
    case MergeMode::Set:
        m_mask = mask;
        break;
    case MergeMode::Add:
        m_mask.add(mask);
        break;
    case MergeMode::Subtract:
        m_mask.subtract(mask);
        break;
    case MergeMode::Intersect:
        m_mask.intersect(mask);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

}
