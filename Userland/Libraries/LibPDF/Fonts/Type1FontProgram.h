/*
 * Copyright (c) 2023, Rodrigo Tobar <rtobarc@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/SourceLocation.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Path.h>
#include <LibPDF/Encoding.h>
#include <LibPDF/Error.h>

namespace PDF {

class Encoding;

class Type1FontProgram : public RefCounted<Type1FontProgram> {

public:
    RefPtr<Gfx::Bitmap> rasterize_glyph(u32 char_code, float width, Gfx::GlyphSubpixelOffset subpixel_offset);
    Gfx::FloatPoint glyph_translation(u32 char_code, float width) const;
    RefPtr<Encoding> encoding() const { return m_encoding; }

protected:
    struct Glyph {
        Gfx::Path path;
        float width;
    };

    struct GlyphParserState {
        Glyph glyph;

        Gfx::FloatPoint point;

        bool flex_feature { false };
        size_t flex_index;
        Array<float, 14> flex_sequence;

        size_t sp { 0 };
        Array<float, 24> stack;

        size_t postscript_sp { 0 };
        Array<float, 24> postscript_stack;
    };

    static PDFErrorOr<Glyph> parse_glyph(ReadonlyBytes const&, Vector<ByteBuffer> const&, GlyphParserState&);

    static Error error(
        DeprecatedString const& message
#ifdef PDF_DEBUG
        ,
        SourceLocation loc = SourceLocation::current()
#endif
    );

    void set_encoding(RefPtr<Encoding>&& encoding)
    {
        m_encoding = move(encoding);
    }

    void set_font_matrix(Gfx::AffineTransform&& font_matrix)
    {
        m_font_matrix = move(font_matrix);
    }

    PDFErrorOr<void> add_glyph(u16 char_code, Glyph&& glyph)
    {
        TRY(m_glyph_map.try_set(char_code, glyph));
        return {};
    }

private:
    HashMap<u16, Glyph> m_glyph_map;
    Gfx::AffineTransform m_font_matrix;
    RefPtr<Encoding> m_encoding;

    Gfx::Path build_char(u32 char_code, float width, Gfx::GlyphSubpixelOffset subpixel_offset);
    Gfx::AffineTransform glyph_transform_to_device_space(Glyph const& glyph, float width) const;
};

}
