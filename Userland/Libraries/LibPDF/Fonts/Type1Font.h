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

class Type1Font : public SimpleFont {
public:
    void draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u8 char_code, Color color) override;
    Type type() const override { return PDFFont::Type::Type1; }

protected:
    PDFErrorOr<void> initialize(Document*, NonnullRefPtr<DictObject> const&, float font_size) override;

private:
    RefPtr<Type1FontProgram> m_font_program;
    RefPtr<Gfx::Font> m_font;
    HashMap<Gfx::GlyphIndexWithSubpixelOffset, RefPtr<Gfx::Bitmap>> m_glyph_cache;
};

}
