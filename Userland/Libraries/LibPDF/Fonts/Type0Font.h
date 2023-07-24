/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibGfx/Point.h>
#include <LibPDF/Fonts/PDFFont.h>

namespace PDF {

class CIDFontType;

struct CIDSystemInfo {
    DeprecatedString registry;
    DeprecatedString ordering;
    u8 supplement;
};

class Type0Font : public PDFFont {
public:
    Type0Font();
    ~Type0Font();

    void set_font_size(float font_size) override;
    PDFErrorOr<Gfx::FloatPoint> draw_string(Gfx::Painter&, Gfx::FloatPoint pos, DeprecatedString const&, Color const&, float, float, float, float) override;
    Type type() const override { return PDFFont::Type::Type0; }

protected:
    PDFErrorOr<void> initialize(Document*, NonnullRefPtr<DictObject> const&, float) override;

private:
    float get_char_width(u16 char_code) const;

    CIDSystemInfo m_system_info;
    HashMap<u16, u16> m_widths;
    u16 m_missing_width;
    OwnPtr<CIDFontType> m_cid_font_type;
};

}
