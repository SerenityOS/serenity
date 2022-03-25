/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibPDF/Document.h>

namespace PDF {

class PDFFont : public RefCounted<PDFFont> {
public:
    static PDFErrorOr<NonnullRefPtr<PDFFont>> create(Document*, NonnullRefPtr<DictObject>);

    virtual ~PDFFont() = default;

    virtual u32 char_code_to_code_point(u16 char_code) const = 0;
    virtual float get_char_width(u16 char_code, float font_size) const = 0;
};

}
