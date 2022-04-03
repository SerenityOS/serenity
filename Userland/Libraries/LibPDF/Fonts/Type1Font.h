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
    // Also used by TrueTypeFont, which is very similar to Type1
    struct Data {
        RefPtr<StreamObject> to_unicode;
        NonnullRefPtr<Encoding> encoding;
        HashMap<u16, u16> widths;
        u16 missing_width;
    };

    static PDFErrorOr<Data> parse_data(Document*, NonnullRefPtr<DictObject> font_dict);

    static PDFErrorOr<NonnullRefPtr<Type1Font>> create(Document*, NonnullRefPtr<DictObject>);

    Type1Font(Data);
    ~Type1Font() override = default;

    u32 char_code_to_code_point(u16 char_code) const override;
    float get_char_width(u16 char_code, float font_size) const override;

private:
    Data m_data;
};

}
