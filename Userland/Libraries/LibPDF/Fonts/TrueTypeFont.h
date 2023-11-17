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
    Optional<float> get_glyph_width(u8 char_code) const override;
    void set_font_size(float font_size) override;
    PDFErrorOr<void> draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float, u8, Renderer const&) override;

    DeprecatedFlyString base_font_name() const { return m_base_font_name; }

protected:
    PDFErrorOr<void> initialize(Document*, NonnullRefPtr<DictObject> const&, float font_size) override;

private:
    DeprecatedFlyString m_base_font_name;
    RefPtr<Gfx::Font> m_font;
    bool m_encoding_is_mac_roman_or_win_ansi { false };
    Optional<u8> m_high_byte;
};

}
