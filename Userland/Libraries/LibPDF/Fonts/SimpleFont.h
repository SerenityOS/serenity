/*
 * Copyright (c) 2023, Rodrigo Tobar <rtobarc@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Point.h>
#include <LibPDF/Fonts/PDFFont.h>

namespace PDF {

class SimpleFont : public PDFFont {
public:
    PDFErrorOr<Gfx::FloatPoint> draw_string(Gfx::Painter&, Gfx::FloatPoint, ByteString const&, Renderer const&) override;

protected:
    PDFErrorOr<void> initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size) override;
    virtual Optional<float> get_glyph_width(u8 char_code) const = 0;
    virtual PDFErrorOr<void> draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u8 char_code, Renderer const&) = 0;
    RefPtr<Encoding>& encoding() { return m_encoding; }
    RefPtr<Encoding> const& encoding() const { return m_encoding; }

    Gfx::AffineTransform& font_matrix() { return m_font_matrix; }

private:
    RefPtr<Encoding> m_encoding;
    RefPtr<StreamObject> m_to_unicode;
    HashMap<u8, u16> m_widths;
    u16 m_missing_width { 0 };

    // "For all font types except Type 3, the units of glyph space are one-thousandth of a unit of text space;
    // for a Type 3 font, the transformation from glyph space to text space is defined by a font matrix specified
    // in an explicit FontMatrix entry in the font."
    Gfx::AffineTransform m_font_matrix { 1.0f / 1000.0f, 0, 0, 1.0f / 1000.0f, 0, 0 };
};

}
