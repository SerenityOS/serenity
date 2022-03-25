/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/PDFFont.h>
#include <LibPDF/Fonts/Type1Font.h>

namespace PDF {

PDFErrorOr<NonnullRefPtr<PDFFont>> PDFFont::create(Document* document, NonnullRefPtr<DictObject> dict)
{
    auto subtype = TRY(dict->get_name(document, CommonNames::Subtype))->name();

    if (subtype == "Type1")
        return TRY(Type1Font::create(document, dict));

    dbgln("Unknown font subtype: {}", subtype);
    TODO();
}

}
