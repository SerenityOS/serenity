/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/VectorFont.h>

#define POINTS_PER_INCH 72.0f
#define DEFAULT_DPI 96

namespace Gfx {

struct GlyphIndexWithSubpixelOffset {
    u32 glyph_id;
    GlyphSubpixelOffset subpixel_offset;

    bool operator==(GlyphIndexWithSubpixelOffset const&) const = default;
};

class ScaledFont : public Gfx::Font {
public:
    ScaledFont(NonnullRefPtr<VectorFont> font, float point_width, float point_height, unsigned dpi_x = DEFAULT_DPI, unsigned dpi_y = DEFAULT_DPI)
        : m_font(move(font))
        , m_point_width(point_width)
        , m_point_height(point_height)
    {
        float units_per_em = m_font->units_per_em();
        m_x_scale = (point_width * dpi_x) / (POINTS_PER_INCH * units_per_em);
        m_y_scale = (point_height * dpi_y) / (POINTS_PER_INCH * units_per_em);
    }
    u32 glyph_id_for_code_point(u32 code_point) const { return m_font->glyph_id_for_code_point(code_point); }
    ScaledFontMetrics metrics() const { return m_font->metrics(m_x_scale, m_y_scale); }
    ScaledGlyphMetrics glyph_metrics(u32 glyph_id) const { return m_font->glyph_metrics(glyph_id, m_x_scale, m_y_scale); }
    RefPtr<Gfx::Bitmap> rasterize_glyph(u32 glyph_id, GlyphSubpixelOffset) const;

    // ^Gfx::Font
    virtual NonnullRefPtr<Font> clone() const override { return MUST(try_clone()); } // FIXME: clone() should not need to be implemented
    virtual ErrorOr<NonnullRefPtr<Font>> try_clone() const override { return *this; }
    virtual u8 presentation_size() const override { return m_point_height; }
    virtual float pixel_size() const override { return m_point_height * 1.33333333f; }
    virtual float point_size() const override { return m_point_height; }
    virtual Gfx::FontPixelMetrics pixel_metrics() const override;
    virtual u8 slope() const override { return m_font->slope(); }
    virtual u16 weight() const override { return m_font->weight(); }
    virtual Gfx::Glyph glyph(u32 code_point) const override;
    virtual float glyph_left_bearing(u32 code_point) const override;
    virtual Glyph glyph(u32 code_point, GlyphSubpixelOffset) const override;
    virtual bool contains_glyph(u32 code_point) const override { return m_font->glyph_id_for_code_point(code_point) > 0; }
    virtual float glyph_width(u32 code_point) const override;
    virtual float glyph_or_emoji_width(u32 code_point) const override;
    virtual float glyphs_horizontal_kerning(u32 left_code_point, u32 right_code_point) const override;
    virtual int preferred_line_height() const override { return metrics().height() + metrics().line_gap; }
    virtual u8 glyph_height() const override { return m_point_height; }
    virtual int x_height() const override { return m_point_height; }      // FIXME: Read from font
    virtual u8 min_glyph_width() const override { return 1; }             // FIXME: Read from font
    virtual u8 max_glyph_width() const override { return m_point_width; } // FIXME: Read from font
    virtual u8 glyph_fixed_width() const override;
    virtual u8 baseline() const override { return m_point_height; }  // FIXME: Read from font
    virtual u8 mean_line() const override { return m_point_height; } // FIXME: Read from font
    virtual float width(StringView) const override;
    virtual float width(Utf8View const&) const override;
    virtual float width(Utf32View const&) const override;
    virtual DeprecatedString name() const override { return DeprecatedString::formatted("{} {}", family(), variant()); }
    virtual bool is_fixed_width() const override { return m_font->is_fixed_width(); }
    virtual u8 glyph_spacing() const override { return 0; }
    virtual size_t glyph_count() const override { return m_font->glyph_count(); }
    virtual DeprecatedString family() const override { return m_font->family(); }
    virtual DeprecatedString variant() const override { return m_font->variant(); }
    virtual DeprecatedString qualified_name() const override { return DeprecatedString::formatted("{} {} {} {}", family(), presentation_size(), weight(), slope()); }
    virtual DeprecatedString human_readable_name() const override { return DeprecatedString::formatted("{} {} {}", family(), variant(), presentation_size()); }

private:
    NonnullRefPtr<VectorFont> m_font;
    float m_x_scale { 0.0f };
    float m_y_scale { 0.0f };
    float m_point_width { 0.0f };
    float m_point_height { 0.0f };
    mutable HashMap<GlyphIndexWithSubpixelOffset, RefPtr<Gfx::Bitmap>> m_cached_glyph_bitmaps;

    template<typename T>
    float unicode_view_width(T const& view) const;
};

}

namespace AK {

template<>
struct Traits<Gfx::GlyphIndexWithSubpixelOffset> : public GenericTraits<Gfx::GlyphIndexWithSubpixelOffset> {
    static unsigned hash(Gfx::GlyphIndexWithSubpixelOffset const& index)
    {
        return pair_int_hash(index.glyph_id, (index.subpixel_offset.x << 8) | index.subpixel_offset.y);
    }
};

}
