/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GaussBlur3.h"
#include "../FilterParams.h"

namespace PixelPaint::Filters {

void GaussBlur3::apply() const
{
    if (!m_editor)
        return;
    if (auto* layer = m_editor->active_layer()) {
        Gfx::SpatialGaussianBlurFilter<3> filter;
        if (auto parameters = PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<3>>::get()) {
            filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
            layer->did_modify_bitmap(layer->rect());
            m_editor->did_complete_action();
        }
    }
}

}
