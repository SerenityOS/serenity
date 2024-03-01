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

class TrueTypePainter {
public:
    static NonnullOwnPtr<TrueTypePainter> create(Document*, NonnullRefPtr<DictObject> const&, SimpleFont const& containing_pdf_font, AK::NonnullRefPtr<Gfx::ScaledFont>, NonnullRefPtr<Encoding>, bool is_zapf_dingbats);

    PDFErrorOr<void> draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float width, u8 char_code, Renderer const&);
    Optional<float> get_glyph_width(u8 char_code) const;
    void set_font_size(float font_size);

private:
    TrueTypePainter(AK::NonnullRefPtr<Gfx::ScaledFont>, NonnullRefPtr<Encoding>, bool encoding_is_mac_roman_or_win_ansi, bool is_nonsymbolic, Optional<u8> high_byte, bool is_zapf_dingbats);

    NonnullRefPtr<Gfx::ScaledFont> m_font;
    NonnullRefPtr<Encoding> m_encoding;
    bool m_encoding_is_mac_roman_or_win_ansi { false };
    bool m_is_nonsymbolic { false };
    Optional<u8> m_high_byte;
    bool m_is_zapf_dingbats { false };
};

class TrueTypeFont : public SimpleFont {
public:
    Optional<float> get_glyph_width(u8 char_code) const override;
    void set_font_size(float font_size) override;
    PDFErrorOr<void> draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float, u8, Renderer const&) override;

    DeprecatedFlyString base_font_name() const { return m_base_font_name; }

protected:
    PDFErrorOr<void> initialize(Document*, NonnullRefPtr<DictObject> const&, float font_size) override;

private:
    DeprecatedFlyString m_base_font_name;

    // Always non-null once initialize() has completed.
    // FIXME: Move this class hierarchy to the usual fallible construction pattern and make this a NonnullOwnPtr.
    OwnPtr<TrueTypePainter> m_font_painter;
};

}
