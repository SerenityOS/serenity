/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Grayscale.h"
#include "../FilterParams.h"

namespace PixelPaint::Filters {

void Grayscale::apply() const
{
    if (!m_editor)
        return;
    if (auto* layer = m_editor->active_layer()) {
        Gfx::GrayscaleFilter filter;
        filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect());
        layer->did_modify_bitmap(layer->rect());
        m_editor->did_complete_action();
    }
}

}
