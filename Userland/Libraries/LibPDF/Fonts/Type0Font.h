/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibPDF/Fonts/PDFFont.h>

namespace PDF {

struct CIDSystemInfo {
    String registry;
    String ordering;
    u8 supplement;
};

class Type0Font : public PDFFont {
public:
    static PDFErrorOr<NonnullRefPtr<Type0Font>> create(Document*, NonnullRefPtr<DictObject>);

    Type0Font(CIDSystemInfo const&, HashMap<u16, u16> const& widths, u16 missing_width);
    ~Type0Font() override = default;

    u32 char_code_to_code_point(u16 char_code) const override;
    float get_char_width(u16 char_code, float font_size) const override;

private:
    CIDSystemInfo m_system_info;
    HashMap<u16, u16> m_widths;
    u16 m_missing_width;
};

}
