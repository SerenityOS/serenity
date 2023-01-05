/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Font/ScaledFont.h>
#include <LibPDF/Fonts/PDFFont.h>
#include <LibPDF/Fonts/PS1FontProgram.h>

namespace PDF {

class Type1Font : public PDFFont {
public:
    struct Data : PDFFont::CommonData {
        RefPtr<PS1FontProgram> font_program;
    };

    static PDFErrorOr<Data> parse_data(Document*, NonnullRefPtr<DictObject> font_dict, float font_size);

    static PDFErrorOr<NonnullRefPtr<Type1Font>> create(Document*, NonnullRefPtr<DictObject>, float font_size);

    Type1Font(Data);
    ~Type1Font() override = default;

    u32 char_code_to_code_point(u16 char_code) const override;
    float get_char_width(u16 char_code) const override;

    void draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u32 char_code, Color color) override;

    Type type() const override { return PDFFont::Type::Type1; }

private:
    Data m_data;
    HashMap<Gfx::GlyphIndexWithSubpixelOffset, RefPtr<Gfx::Bitmap>> m_glyph_cache;
};

}
