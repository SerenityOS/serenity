/*
 * Copyright (c) 2024, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/Command.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Painting {

void DrawGlyphRun::translate_by(Gfx::IntPoint const& offset)
{
    for (auto& glyph : glyph_run) {
        glyph.visit([&](auto& glyph) {
            glyph.translate_by(offset.to_type<float>());
        });
    }
    rect.translate_by(offset);
}

Gfx::IntRect PaintOuterBoxShadow::bounding_rect() const
{
    return get_outer_box_shadow_bounding_rect(outer_box_shadow_params);
}

void PaintOuterBoxShadow::translate_by(Gfx::IntPoint const& offset)
{
    outer_box_shadow_params.device_content_rect.translate_by(offset.to_type<DevicePixels>());
}

void PaintInnerBoxShadow::translate_by(Gfx::IntPoint const& offset)
{
    outer_box_shadow_params.device_content_rect.translate_by(offset.to_type<DevicePixels>());
}

}
