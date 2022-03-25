/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibPDF/Encoding.h>
#include <LibPDF/Fonts/PDFFont.h>

namespace PDF {

class Type1Font : public PDFFont {
public:
    static PDFErrorOr<NonnullRefPtr<Type1Font>> create(Document*, NonnullRefPtr<DictObject>);

    Type1Font(RefPtr<StreamObject> to_unicode, NonnullRefPtr<Encoding>, HashMap<u16, u16> const& m_widths, u16 missing_width);
    ~Type1Font() override = default;

    u32 char_code_to_code_point(u16 char_code) const override;
    float get_char_width(u16 char_code, float font_size) const override;

private:
    RefPtr<StreamObject> m_to_unicode;
    NonnullRefPtr<Encoding> m_encoding;
    HashMap<u16, u16> m_widths;
    u16 m_missing_width;
};

}
