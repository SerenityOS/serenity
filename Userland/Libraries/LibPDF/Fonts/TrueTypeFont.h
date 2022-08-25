/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Font/TrueType/Font.h>
#include <LibPDF/Fonts/Type1Font.h>

namespace PDF {

class TrueTypeFont : public PDFFont {
public:
    static PDFErrorOr<NonnullRefPtr<PDFFont>> create(Document*, NonnullRefPtr<DictObject>);

    TrueTypeFont(NonnullRefPtr<TTF::Font> ttf_font, Type1Font::Data);
    ~TrueTypeFont() override = default;

    u32 char_code_to_code_point(u16 char_code) const override;
    float get_char_width(u16 char_code, float font_size) const override;

    void draw_glyph(Gfx::Painter&, Gfx::IntPoint const&, float, u32, Color) override {};

    Type type() const override { return PDFFont::Type::TrueType; }

private:
    NonnullRefPtr<TTF::Font> m_ttf_font;
    Type1Font::Data m_data;
};

}
