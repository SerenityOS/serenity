/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibPDF/Encoding.h>
#include <LibPDF/ObjectDerivatives.h>

namespace PDF {

class PDFFont : public RefCounted<PDFFont> {
public:
    static PDFErrorOr<NonnullRefPtr<PDFFont>> create(Document*, NonnullRefPtr<DictObject>);

    virtual ~PDFFont() = default;

    virtual u32 char_code_to_code_point(u16 char_code) const = 0;
};

class Type1Font : public PDFFont {
public:
    static PDFErrorOr<NonnullRefPtr<Type1Font>> create(Document*, NonnullRefPtr<DictObject>);

    Type1Font(RefPtr<StreamObject> to_unicode, NonnullRefPtr<Encoding>);
    ~Type1Font() override = default;

    u32 char_code_to_code_point(u16 char_code) const override;

private:
    RefPtr<StreamObject> m_to_unicode;
    NonnullRefPtr<Encoding> m_encoding;
};

}
