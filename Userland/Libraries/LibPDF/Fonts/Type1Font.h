/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Font/ScaledFont.h>
#include <LibPDF/Fonts/SimpleFont.h>
#include <LibPDF/Fonts/TrueTypeFont.h>
#include <LibPDF/Fonts/Type1FontProgram.h>

namespace PDF {

struct CachedGlyphBitmapsKey {
    u32 glyph_id;
    Gfx::GlyphSubpixelOffset subpixel_offset;
    float width;

    bool operator==(CachedGlyphBitmapsKey const&) const = default;
};

class Type1Font : public SimpleFont {
public:
    Optional<float> get_glyph_width(u8 char_code) const override;
    void set_font_size(float font_size) override;
    PDFErrorOr<void> draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u8 char_code, Renderer const&) override;

    virtual PDFErrorOr<void> append_glyph_path(Gfx::Path& path, Gfx::FloatPoint point, u8 char_code, Renderer const& renderer) override;

    DeprecatedFlyString base_font_name() const { return m_base_font_name; }

protected:
    PDFErrorOr<void> initialize(Document*, NonnullRefPtr<DictObject> const&, float font_size) override;

private:
    DeprecatedFlyString char_name_for_char_code(u8 char_code) const;

    DeprecatedFlyString m_base_font_name;
    RefPtr<Type1FontProgram> m_font_program;
    OwnPtr<TrueTypePainter> m_fallback_font_painter;
    HashMap<CachedGlyphBitmapsKey, RefPtr<Gfx::Bitmap>> m_cached_glyph_bitmaps;
};

}

namespace AK {

template<>
struct Traits<PDF::CachedGlyphBitmapsKey> : public DefaultTraits<PDF::CachedGlyphBitmapsKey> {
    static unsigned hash(PDF::CachedGlyphBitmapsKey const& index)
    {
        return pair_int_hash(pair_int_hash(index.glyph_id, (index.subpixel_offset.x << 8) | index.subpixel_offset.y), int_hash(bit_cast<u32>(index.width)));
    }
};

}
