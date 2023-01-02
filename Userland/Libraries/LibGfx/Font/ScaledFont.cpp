/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibGfx/Font/ScaledFont.h>

namespace Gfx {

float ScaledFont::width(StringView view) const { return unicode_view_width(Utf8View(view)); }
float ScaledFont::width(Utf8View const& view) const { return unicode_view_width(view); }
float ScaledFont::width(Utf32View const& view) const { return unicode_view_width(view); }

template<typename T>
ALWAYS_INLINE float ScaledFont::unicode_view_width(T const& view) const
{
    if (view.is_empty())
        return 0;
    float width = 0;
    float longest_width = 0;
    u32 last_code_point = 0;
    for (auto code_point : view) {
        if (code_point == '\n' || code_point == '\r') {
            longest_width = max(width, longest_width);
            width = 0;
            last_code_point = code_point;
            continue;
        }
        u32 glyph_id = glyph_id_for_code_point(code_point);
        auto kerning = glyphs_horizontal_kerning(last_code_point, code_point);
        width += kerning + glyph_metrics(glyph_id).advance_width;
        last_code_point = code_point;
    }
    longest_width = max(width, longest_width);
    return longest_width;
}

RefPtr<Gfx::Bitmap> ScaledFont::rasterize_glyph(u32 glyph_id, GlyphSubpixelOffset subpixel_offset) const
{
    GlyphIndexWithSubpixelOffset index { glyph_id, subpixel_offset };
    auto glyph_iterator = m_cached_glyph_bitmaps.find(index);
    if (glyph_iterator != m_cached_glyph_bitmaps.end())
        return glyph_iterator->value;

    auto glyph_bitmap = m_font->rasterize_glyph(glyph_id, m_x_scale, m_y_scale, subpixel_offset);
    m_cached_glyph_bitmaps.set(index, glyph_bitmap);
    return glyph_bitmap;
}

Gfx::Glyph ScaledFont::glyph(u32 code_point) const
{
    return glyph(code_point, GlyphSubpixelOffset { 0, 0 });
}

Gfx::Glyph ScaledFont::glyph(u32 code_point, GlyphSubpixelOffset subpixel_offset) const
{
    auto id = glyph_id_for_code_point(code_point);
    auto bitmap = rasterize_glyph(id, subpixel_offset);
    auto metrics = glyph_metrics(id);
    return Gfx::Glyph(bitmap, metrics.left_side_bearing, metrics.advance_width, metrics.ascender);
}

float ScaledFont::glyph_left_bearing(u32 code_point) const
{
    auto id = glyph_id_for_code_point(code_point);
    return glyph_metrics(id).left_side_bearing;
}

float ScaledFont::glyph_width(u32 code_point) const
{
    auto id = glyph_id_for_code_point(code_point);
    auto metrics = glyph_metrics(id);
    return metrics.advance_width;
}

float ScaledFont::glyph_or_emoji_width(u32 code_point) const
{
    auto id = glyph_id_for_code_point(code_point);
    auto metrics = glyph_metrics(id);
    return metrics.advance_width;
}

float ScaledFont::glyphs_horizontal_kerning(u32 left_code_point, u32 right_code_point) const
{
    if (left_code_point == 0 || right_code_point == 0)
        return 0.f;

    auto left_glyph_id = glyph_id_for_code_point(left_code_point);
    auto right_glyph_id = glyph_id_for_code_point(right_code_point);
    if (left_glyph_id == 0 || right_glyph_id == 0)
        return 0.f;

    return m_font->glyphs_horizontal_kerning(left_glyph_id, right_glyph_id, m_x_scale);
}

u8 ScaledFont::glyph_fixed_width() const
{
    return glyph_metrics(glyph_id_for_code_point(' ')).advance_width;
}

Gfx::FontPixelMetrics ScaledFont::pixel_metrics() const
{
    auto metrics = m_font->metrics(m_x_scale, m_y_scale);

    return Gfx::FontPixelMetrics {
        .size = (float)pixel_size(),
        .x_height = (float)x_height(),
        .advance_of_ascii_zero = (float)glyph_width('0'),
        .glyph_spacing = (float)glyph_spacing(),
        .ascent = metrics.ascender,
        .descent = -metrics.descender,
        .line_gap = metrics.line_gap,
    };
}

}
