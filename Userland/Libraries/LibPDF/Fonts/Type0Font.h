/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Point.h>
#include <LibPDF/Fonts/PDFFont.h>

namespace PDF {

struct CIDSystemInfo {
    DeprecatedString registry;
    DeprecatedString ordering;
    u8 supplement;
};

class Type0Font : public PDFFont {
public:
    static PDFErrorOr<NonnullRefPtr<Type0Font>> create(Document*, NonnullRefPtr<DictObject>);

    Type0Font(CIDSystemInfo const&, HashMap<u16, u16> const& widths, u16 missing_width);
    ~Type0Font() override = default;

    float get_char_width(u16 char_code) const override;

    void draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float, u32, Color) override {};

    Type type() const override { return PDFFont::Type::Type0; }
    DeprecatedFlyString base_font_name() const override { return ""; }

private:
    CIDSystemInfo m_system_info;
    HashMap<u16, u16> m_widths;
    u16 m_missing_width;
};

}
