/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SipHash.h>
#include <LibGfx/Font/ScaledFont.h>
#include <LibPDF/Fonts/SimpleFont.h>
#include <LibPDF/Fonts/Type1FontProgram.h>

namespace PDF {

struct [[gnu::packed]] Type1GlyphCacheKey {
    u32 glyph_id;
    float width;
    Gfx::GlyphSubpixelOffset subpixel_offset;

    bool operator==(Type1GlyphCacheKey const&) const = default;
};

class Type1Font : public SimpleFont {
public:
    Optional<float> get_glyph_width(u8 char_code) const override;
    void set_font_size(float font_size) override;
    PDFErrorOr<void> draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u8 char_code, Renderer const&) override;

    DeprecatedFlyString base_font_name() const { return m_base_font_name; }

protected:
    PDFErrorOr<void> initialize(Document*, NonnullRefPtr<DictObject> const&, float font_size) override;

private:
    DeprecatedFlyString m_base_font_name;
    RefPtr<Type1FontProgram> m_font_program;
    RefPtr<Gfx::Font> m_font;
    HashMap<Type1GlyphCacheKey, RefPtr<Gfx::Bitmap>> m_glyph_cache;
};

}

namespace AK {

template<>
struct Traits<PDF::Type1GlyphCacheKey> : public DefaultTraits<PDF::Type1GlyphCacheKey> {
    static unsigned hash(PDF::Type1GlyphCacheKey const& index)
    {
        return standard_sip_hash_trivial(index);
    }
};

}
