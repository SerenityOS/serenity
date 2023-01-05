/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGfx/Font/OpenType/Font.h>
#include <LibPDF/Fonts/PDFFont.h>

namespace PDF {

class TrueTypeFont : public PDFFont {
public:
    static PDFErrorOr<PDFFont::CommonData> parse_data(Document* document, NonnullRefPtr<DictObject> dict, float font_size);

    static PDFErrorOr<NonnullRefPtr<PDFFont>> create(Document*, NonnullRefPtr<DictObject>, float font_size);

    TrueTypeFont(PDFFont::CommonData);
    ~TrueTypeFont() override = default;

    u32 char_code_to_code_point(u16 char_code) const override;
    float get_char_width(u16 char_code) const override;

    void draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float, u32, Color) override;

    Type type() const override { return PDFFont::Type::TrueType; }

private:
    PDFFont::CommonData m_data;
};

}
