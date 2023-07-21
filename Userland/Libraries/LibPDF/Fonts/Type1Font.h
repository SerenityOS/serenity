/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Font/ScaledFont.h>
#include <LibPDF/Fonts/SimpleFont.h>
#include <LibPDF/Fonts/Type1FontProgram.h>

namespace PDF {

struct Type1GlyphCacheKey {
    u32 glyph_id;
    Gfx::GlyphSubpixelOffset subpixel_offset;
    float width;

    bool operator==(Type1GlyphCacheKey const&) const = default;
};

class Type1Font : public SimpleFont {
public:
    float get_glyph_width(u8 char_code) const override;
    void set_font_size(float font_size) override;
    void draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u8 char_code, Color color) override;
    Type type() const override { return PDFFont::Type::Type1; }

protected:
    PDFErrorOr<void> initialize(Document*, NonnullRefPtr<DictObject> const&, float font_size) override;

private:
    RefPtr<Type1FontProgram> m_font_program;
    RefPtr<Gfx::Font> m_font;
    HashMap<Type1GlyphCacheKey, RefPtr<Gfx::Bitmap>> m_glyph_cache;
};

}

namespace AK {

template<>
struct Traits<PDF::Type1GlyphCacheKey> : public GenericTraits<PDF::Type1GlyphCacheKey> {
    static unsigned hash(PDF::Type1GlyphCacheKey const& index)
    {
        return pair_int_hash(pair_int_hash(index.glyph_id, (index.subpixel_offset.x << 8) | index.subpixel_offset.y), int_hash(bit_cast<u32>(index.width)));
    }
};

}
