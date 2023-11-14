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
    PDFErrorOr<Gfx::FloatPoint> draw_string(Gfx::Painter&, Gfx::FloatPoint, DeprecatedString const&, Renderer const&) override;

    DeprecatedFlyString base_font_name() const { return m_base_font_name; }

protected:
    PDFErrorOr<void> initialize(Document*, NonnullRefPtr<DictObject> const&, float) override;

private:
    float get_char_width(u16 char_code) const;

    DeprecatedFlyString m_base_font_name;
    CIDSystemInfo m_system_info;
    HashMap<u16, u16> m_widths;
    u16 m_missing_width;
    OwnPtr<CIDFontType> m_cid_font_type;
};

}
