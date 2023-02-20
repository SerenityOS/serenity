/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/ByteReader.h>
#include <AK/DeprecatedString.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Size.h>

namespace Gfx {

// FIXME: Make a MutableGlyphBitmap buddy class for FontEditor instead?
class GlyphBitmap {
public:
    GlyphBitmap() = default;
    GlyphBitmap(u8 const* rows, size_t start_index, IntSize size)
        : m_rows(rows)
        , m_start_index(start_index)
        , m_size(size)
    {
    }

    unsigned row(unsigned index) const { return ByteReader::load32(bitmap(index).data()); }

    bool bit_at(int x, int y) const { return bitmap(y).get(x); }
    void set_bit_at(int x, int y, bool b) { bitmap(y).set(x, b); }

    IntSize size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }

    static constexpr size_t bytes_per_row() { return sizeof(u32); }
    static constexpr int max_width() { return bytes_per_row() * 8; }
    static constexpr int max_height() { return max_width() + bytes_per_row(); }

private:
    AK::Bitmap bitmap(size_t y) const
    {
        return { const_cast<u8*>(m_rows) + bytes_per_row() * (m_start_index + y), bytes_per_row() * 8 };
    }

    u8 const* m_rows { nullptr };
    size_t m_start_index { 0 };
    IntSize m_size { 0, 0 };
};

class Glyph {
public:
    Glyph(GlyphBitmap const& glyph_bitmap, float left_bearing, float advance, float ascent)
        : m_glyph_bitmap(glyph_bitmap)
        , m_left_bearing(left_bearing)
        , m_advance(advance)
        , m_ascent(ascent)
    {
    }

    Glyph(RefPtr<Bitmap> bitmap, float left_bearing, float advance, float ascent)
        : m_bitmap(bitmap)
        , m_left_bearing(left_bearing)
        , m_advance(advance)
        , m_ascent(ascent)
    {
    }

    bool is_glyph_bitmap() const { return !m_bitmap; }
    GlyphBitmap glyph_bitmap() const { return m_glyph_bitmap; }
    RefPtr<Bitmap> bitmap() const { return m_bitmap; }
    float left_bearing() const { return m_left_bearing; }
    float advance() const { return m_advance; }
    float ascent() const { return m_ascent; }

private:
    GlyphBitmap m_glyph_bitmap;
    RefPtr<Bitmap> m_bitmap;
    float m_left_bearing;
    float m_advance;
    float m_ascent;
};

struct GlyphSubpixelOffset {
    u8 x;
    u8 y;

    // TODO: Allow setting this at runtime via some config?
    static constexpr int subpixel_divisions() { return 3; }
    FloatPoint to_float_point() const { return FloatPoint(x / float(subpixel_divisions()), y / float(subpixel_divisions())); }

    bool operator==(GlyphSubpixelOffset const&) const = default;
};

struct GlyphRasterPosition {
    // Where the glyph bitmap should be drawn/blitted.
    IntPoint blit_position;

    // A subpixel offset to be used when rendering the glyph.
    // This improves kerning and alignment at the expense of caching a few extra bitmaps.
    // This is (currently) snapped to thirds of a subpixel (i.e. 0, 0.33, 0.66).
    GlyphSubpixelOffset subpixel_offset;

    static GlyphRasterPosition get_nearest_fit_for(FloatPoint position);
};

struct FontPixelMetrics {
    float size { 0 };
    float x_height { 0 };
    float advance_of_ascii_zero { 0 };
    float glyph_spacing { 0 };

    // Number of pixels the font extends above the baseline.
    float ascent { 0 };

    // Number of pixels the font descends below the baseline.
    float descent { 0 };

    // Line gap specified by font.
    float line_gap { 0 };

    float line_spacing() const { return ascent + descent + line_gap; }
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/os2#uswidthclass
enum FontWidth {
    UltraCondensed = 1,
    ExtraCondensed = 2,
    Condensed = 3,
    SemiCondensed = 4,
    Normal = 5,
    SemiExpanded = 6,
    Expanded = 7,
    ExtraExpanded = 8,
    UltraExpanded = 9
};

class Font : public RefCounted<Font> {
public:
    enum class AllowInexactSizeMatch {
        No,
        Yes,
    };

    virtual NonnullRefPtr<Font> clone() const = 0;
    virtual ErrorOr<NonnullRefPtr<Font>> try_clone() const = 0;
    virtual ~Font() {};

    virtual FontPixelMetrics pixel_metrics() const = 0;

    virtual u8 presentation_size() const = 0;
    virtual float pixel_size() const = 0;
    virtual u8 slope() const = 0;

    virtual u16 width() const = 0;

    virtual u16 weight() const = 0;
    virtual Glyph glyph(u32 code_point) const = 0;
    virtual Glyph glyph(u32 code_point, GlyphSubpixelOffset) const = 0;
    virtual bool contains_glyph(u32 code_point) const = 0;

    virtual float glyph_left_bearing(u32 code_point) const = 0;
    virtual float glyph_width(u32 code_point) const = 0;
    virtual float glyph_or_emoji_width(u32 code_point) const = 0;
    virtual float glyphs_horizontal_kerning(u32 left_code_point, u32 right_code_point) const = 0;
    virtual u8 glyph_height() const = 0;
    virtual int x_height() const = 0;
    virtual float preferred_line_height() const = 0;

    virtual u8 min_glyph_width() const = 0;
    virtual u8 max_glyph_width() const = 0;
    virtual u8 glyph_fixed_width() const = 0;

    virtual u8 baseline() const = 0;
    virtual u8 mean_line() const = 0;

    virtual float width(StringView) const = 0;
    virtual float width(Utf8View const&) const = 0;
    virtual float width(Utf32View const&) const = 0;

    virtual DeprecatedString name() const = 0;

    virtual bool is_fixed_width() const = 0;

    virtual u8 glyph_spacing() const = 0;

    virtual size_t glyph_count() const = 0;

    virtual DeprecatedString family() const = 0;
    virtual DeprecatedString variant() const = 0;

    virtual DeprecatedString qualified_name() const = 0;
    virtual DeprecatedString human_readable_name() const = 0;

    virtual RefPtr<Font> with_size(float point_size) const = 0;

    Font const& bold_variant() const;

private:
    mutable RefPtr<Gfx::Font const> m_bold_variant;
};

}
