/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibPDF/Encoding.h>
#include <LibPDF/Fonts/PDFFont.h>
#include <LibPDF/Fonts/PS1FontProgram.h>

namespace PDF {

class Type1Font : public PDFFont {
public:
    // Also used by TrueTypeFont, which is very similar to Type1
    struct Data {
        RefPtr<PS1FontProgram> font_program;
        RefPtr<StreamObject> to_unicode;
        NonnullRefPtr<Encoding> encoding;
        HashMap<u16, u16> widths;
        u16 missing_width;
        bool is_standard_font;
    };

    static PDFErrorOr<Data> parse_data(Document*, NonnullRefPtr<DictObject> font_dict);

    static PDFErrorOr<NonnullRefPtr<Type1Font>> create(Document*, NonnullRefPtr<DictObject>);

    Type1Font(Data);
    ~Type1Font() override = default;

    u32 char_code_to_code_point(u16 char_code) const override;
    float get_char_width(u16 char_code, float font_size) const override;

    void draw_glyph(Gfx::Painter& painter, Gfx::IntPoint const& point, float width, u32 code_point, Color color) override;

    Type type() const override { return PDFFont::Type::Type1; }

private:
    Data m_data;
};

}
