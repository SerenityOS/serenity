/*
 * Copyright (c) 2022, Julian Offenhäuser <offenhaeuser@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Path.h>
#include <LibPDF/Error.h>

namespace PDF {

class Reader;
class Encoding;

class PS1FontProgram : public RefCounted<PS1FontProgram> {
public:
    PDFErrorOr<void> create(ReadonlyBytes const&, RefPtr<Encoding>, size_t cleartext_length, size_t encrypted_length);

    RefPtr<Gfx::Bitmap> rasterize_glyph(u32 char_code, float width, Gfx::GlyphSubpixelOffset);
    Gfx::Path build_char(u32 char_code, float width);

    RefPtr<Encoding> encoding() const { return m_encoding; }
    Gfx::FloatPoint glyph_translation(u32 char_code, float width) const;

private:
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

    Gfx::AffineTransform glyph_transform_to_device_space(Glyph const&, float width) const;

    PDFErrorOr<Glyph> parse_glyph(ReadonlyBytes const&, GlyphParserState&);
    PDFErrorOr<void> parse_encrypted_portion(ByteBuffer const&);
    PDFErrorOr<Vector<ByteBuffer>> parse_subroutines(Reader&);
    PDFErrorOr<Vector<float>> parse_number_array(Reader&, size_t length);
    PDFErrorOr<DeprecatedString> parse_word(Reader&);
    PDFErrorOr<float> parse_float(Reader&);
    PDFErrorOr<int> parse_int(Reader&);

    PDFErrorOr<ByteBuffer> decrypt(ReadonlyBytes const&, u16 key, size_t skip);
    bool seek_name(Reader&, DeprecatedString const&);

    static Error error(
        DeprecatedString const& message
#ifdef PDF_DEBUG
        ,
        SourceLocation loc = SourceLocation::current()
#endif
    );

    Vector<ByteBuffer> m_subroutines;
    Vector<ByteBuffer> m_character_names;
    HashMap<u16, Glyph> m_glyph_map;

    Gfx::AffineTransform m_font_matrix;

    RefPtr<Encoding> m_encoding;

    u16 m_encryption_key { 4330 };
    int m_lenIV { 4 };
};

}
