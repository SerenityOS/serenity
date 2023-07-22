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
    PDFErrorOr<Gfx::FloatPoint> draw_string(Gfx::Painter&, Gfx::FloatPoint, DeprecatedString const&, Color const&, float font_size, float character_spacing, float word_spacing, float horizontal_scaling) override;

protected:
    PDFErrorOr<void> initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size) override;
    virtual float get_glyph_width(u8 char_code) const = 0;
    virtual void draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u8 char_code, Color color) = 0;
    RefPtr<Encoding>& encoding() { return m_encoding; }
    RefPtr<Encoding> const& encoding() const { return m_encoding; }

private:
    RefPtr<Encoding> m_encoding;
    RefPtr<StreamObject> m_to_unicode;
    HashMap<u8, u16> m_widths;
    u16 m_missing_width;
};

}
