/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGfx/Font/OpenType/Font.h>
#include <LibPDF/Fonts/SimpleFont.h>

namespace PDF {

class TrueTypeFont : public SimpleFont {
public:
    float get_glyph_width(u8 char_code) const override;
    void set_font_size(float font_size) override;
    void draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float, u8, Color) override;
    Type type() const override { return PDFFont::Type::TrueType; }

protected:
    PDFErrorOr<void> initialize(Document*, NonnullRefPtr<DictObject> const&, float font_size) override;

private:
    RefPtr<Gfx::Font> m_font;
};

}
