/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/StringView.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Size.h>
#include <LibGfx/TrueTypeFont/Cmap.h>
#include <LibGfx/TrueTypeFont/Glyf.h>
#include <LibGfx/TrueTypeFont/Tables.h>

#define POINTS_PER_INCH 72.0f
#define DEFAULT_DPI 96

namespace TTF {

struct ScaledFontMetrics {
    int ascender;
    int descender;
    int line_gap;
    int advance_width_max;

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

class Font : public RefCounted<Font> {
    AK_MAKE_NONCOPYABLE(Font);

public:
    static ErrorOr<NonnullRefPtr<Font>> try_load_from_file(String path, unsigned index = 0);
    static ErrorOr<NonnullRefPtr<Font>> try_load_from_externally_owned_memory(ReadonlyBytes bytes, unsigned index = 0);

    ScaledFontMetrics metrics(float x_scale, float y_scale) const;
    ScaledGlyphMetrics glyph_metrics(u32 glyph_id, float x_scale, float y_scale) const;
    RefPtr<Gfx::Bitmap> rasterize_glyph(u32 glyph_id, float x_scale, float y_scale) const;
    u32 glyph_count() const;
    u16 units_per_em() const;
    u32 glyph_id_for_code_point(u32 code_point) const { return m_cmap.glyph_id_for_code_point(code_point); }
    String family() const;
    String variant() const;
    u16 weight() const;
    u8 slope() const;
    bool is_fixed_width() const;

private:
    enum class Offsets {
        NumTables = 4,
        TableRecord_Offset = 8,
        TableRecord_Length = 12,
    };
    enum class Sizes {
        TTCHeaderV1 = 12,
        OffsetTable = 12,
        TableRecord = 16,
    };

    static ErrorOr<NonnullRefPtr<Font>> try_load_from_offset(ReadonlyBytes, unsigned index = 0);

    Font(ReadonlyBytes bytes, Head&& head, Name&& name, Hhea&& hhea, Maxp&& maxp, Hmtx&& hmtx, Cmap&& cmap, Loca&& loca, Glyf&& glyf, OS2&& os2)
        : m_buffer(move(bytes))
        , m_head(move(head))
        , m_name(move(name))
        , m_hhea(move(hhea))
        , m_maxp(move(maxp))
        , m_hmtx(move(hmtx))
        , m_loca(move(loca))
        , m_glyf(move(glyf))
        , m_cmap(move(cmap))
        , m_os2(move(os2))
    {
    }

    RefPtr<Core::MappedFile> m_mapped_file;

    ReadonlyBytes m_buffer;

    // These are stateful wrappers around non-owning slices
    Head m_head;
    Name m_name;
    Hhea m_hhea;
    Maxp m_maxp;
    Hmtx m_hmtx;
    Loca m_loca;
    Glyf m_glyf;
    Cmap m_cmap;
    OS2 m_os2;
};

class ScaledFont : public Gfx::Font {
public:
    ScaledFont(NonnullRefPtr<TTF::Font> font, float point_width, float point_height, unsigned dpi_x = DEFAULT_DPI, unsigned dpi_y = DEFAULT_DPI)
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
    RefPtr<Gfx::Bitmap> rasterize_glyph(u32 glyph_id) const;

    // Gfx::Font implementation
    virtual NonnullRefPtr<Font> clone() const override { return *this; } // FIXME: clone() should not need to be implemented
    virtual u8 presentation_size() const override { return m_point_height; }
    virtual u8 slope() const override { return m_font->slope(); }
    virtual u16 weight() const override { return m_font->weight(); }
    virtual Gfx::Glyph glyph(u32 code_point) const override;
    virtual bool contains_glyph(u32 code_point) const override { return m_font->glyph_id_for_code_point(code_point) > 0; }
    virtual u8 glyph_width(u32 code_point) const override;
    virtual int glyph_or_emoji_width(u32 code_point) const override;
    virtual u8 glyph_height() const override { return m_point_height; }
    virtual int x_height() const override { return m_point_height; }      // FIXME: Read from font
    virtual u8 min_glyph_width() const override { return 1; }             // FIXME: Read from font
    virtual u8 max_glyph_width() const override { return m_point_width; } // FIXME: Read from font
    virtual u8 glyph_fixed_width() const override;
    virtual u8 baseline() const override { return m_point_height; }  // FIXME: Read from font
    virtual u8 mean_line() const override { return m_point_height; } // FIXME: Read from font
    virtual int width(StringView) const override;
    virtual int width(Utf8View const&) const override;
    virtual int width(Utf32View const&) const override;
    virtual String name() const override { return String::formatted("{} {}", family(), variant()); }
    virtual bool is_fixed_width() const override { return m_font->is_fixed_width(); }
    virtual u8 glyph_spacing() const override { return m_x_scale; } // FIXME: Read from font
    virtual size_t glyph_count() const override { return m_font->glyph_count(); }
    virtual String family() const override { return m_font->family(); }
    virtual String variant() const override { return m_font->variant(); }
    virtual String qualified_name() const override { return String::formatted("{} {} {} {}", family(), presentation_size(), weight(), slope()); }
    virtual String human_readable_name() const override { return String::formatted("{} {} {}", family(), variant(), presentation_size()); }

private:
    NonnullRefPtr<TTF::Font> m_font;
    float m_x_scale { 0.0f };
    float m_y_scale { 0.0f };
    float m_point_width { 0.0f };
    float m_point_height { 0.0f };
    mutable HashMap<u32, RefPtr<Gfx::Bitmap>> m_cached_glyph_bitmaps;

    template<typename T>
    int unicode_view_width(T const& view) const;
};

}
