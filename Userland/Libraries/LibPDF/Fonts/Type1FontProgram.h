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

namespace PDF {

class Encoding;

class Type1FontProgram : public RefCounted<Type1FontProgram> {

public:
    enum Kind {
        NameKeyed,
        CIDKeyed,
    };

    RefPtr<Gfx::Bitmap> rasterize_glyph(DeprecatedFlyString const& char_name, float width, Gfx::GlyphSubpixelOffset subpixel_offset);
    Gfx::FloatPoint glyph_translation(DeprecatedFlyString const& char_name, float width) const;
    RefPtr<Encoding> encoding() const { return m_encoding; }

    Kind kind() const { return m_kind; }

protected:
    struct AccentedCharacter {
        AccentedCharacter(u8 base_char_code, u8 accent_char_code, float adx, float ady)
            : base_character(Encoding::standard_encoding()->get_name(base_char_code))
            , accent_character(Encoding::standard_encoding()->get_name(accent_char_code))
            , accent_origin(adx, ady)
        {
        }

        DeprecatedFlyString base_character;
        DeprecatedFlyString accent_character;
        Gfx::FloatPoint accent_origin;
    };

    class Glyph {

    public:
        bool has_width() const { return m_width.has_value(); }
        float width() const { return m_width.value(); }
        void set_width(float width)
        {
            m_width = width;
        }

        Gfx::Path& path() { return m_path; }
        Gfx::Path const& path() const { return m_path; }

        bool is_accented_character() const { return m_accented_character.has_value(); }
        AccentedCharacter const& accented_character() const { return m_accented_character.value(); }
        void set_accented_character(AccentedCharacter&& accented_character)
        {
            m_accented_character = move(accented_character);
        }

    private:
        Gfx::Path m_path;
        Optional<float> m_width;
        Optional<AccentedCharacter> m_accented_character;
    };

    struct GlyphParserState {
        Glyph glyph;

        Gfx::FloatPoint point;

        bool flex_feature { false };
        size_t flex_index;
        Array<float, 14> flex_sequence;

        size_t sp { 0 };
        Array<float, 48> stack;
        u8 n_hints { 0 };

        size_t postscript_sp { 0 };
        Array<float, 24> postscript_stack;

        bool is_first_command { true };
    };

    static ErrorOr<Glyph> parse_glyph(ReadonlyBytes const&, Vector<ByteBuffer> const& local_subroutines, Vector<ByteBuffer> const& global_subroutines, GlyphParserState&, bool is_type2);

    void set_encoding(RefPtr<Encoding>&& encoding)
    {
        m_encoding = move(encoding);
    }

    void set_font_matrix(Gfx::AffineTransform&& font_matrix)
    {
        m_font_matrix = move(font_matrix);
    }

    ErrorOr<void> add_glyph(DeprecatedFlyString name, Glyph&& glyph)
    {
        TRY(m_glyph_map.try_set(move(name), move(glyph)));
        return {};
    }

    void consolidate_glyphs();

    void set_kind(Kind kind) { m_kind = kind; }

private:
    HashMap<DeprecatedFlyString, Glyph> m_glyph_map;
    Gfx::AffineTransform m_font_matrix;
    RefPtr<Encoding> m_encoding;
    Kind m_kind { NameKeyed };

    Gfx::Path build_char(DeprecatedFlyString const& char_name, float width, Gfx::GlyphSubpixelOffset subpixel_offset);
    Gfx::AffineTransform glyph_transform_to_device_space(Glyph const& glyph, float width) const;
};

}
