/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <LibGfx/Forward.h>

namespace Gfx {

struct ScaledFontMetrics {
    float ascender { 0 };
    float descender { 0 };
    float line_gap { 0 };

    int height() const
    {
        return ascender - descender;
    }
};

struct ScaledGlyphMetrics {
    int ascender;
    int descender;
    int advance_width;
    int left_side_bearing;
};

class VectorFont : public RefCounted<VectorFont> {
public:
    virtual ~VectorFont() { }
    virtual ScaledFontMetrics metrics(float x_scale, float y_scale) const = 0;
    virtual ScaledGlyphMetrics glyph_metrics(u32 glyph_id, float x_scale, float y_scale) const = 0;
    virtual float glyphs_horizontal_kerning(u32 left_glyph_id, u32 right_glyph_id, float x_scale) const = 0;
    virtual RefPtr<Gfx::Bitmap> rasterize_glyph(u32 glyph_id, float x_scale, float y_scale) const = 0;
    virtual u32 glyph_count() const = 0;
    virtual u16 units_per_em() const = 0;
    virtual u32 glyph_id_for_code_point(u32 code_point) const = 0;
    virtual String family() const = 0;
    virtual String variant() const = 0;
    virtual u16 weight() const = 0;
    virtual u8 slope() const = 0;
    virtual bool is_fixed_width() const = 0;
};

}
