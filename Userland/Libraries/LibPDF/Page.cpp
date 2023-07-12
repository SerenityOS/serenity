/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Document.h>
#include <LibPDF/ObjectDerivatives.h>
#include <LibPDF/Page.h>

namespace PDF {

PDFErrorOr<ByteBuffer> Page::page_contents(Document& document) const
{
    if (contents.is_null())
        return ByteBuffer {};

    // Use our own vector, as the /Content can be an array with multiple
    // streams which gets concatenated.
    // FIXME: Text operators are supposed to only have effects on the current
    // stream object. Do the text operators treat this concatenated stream
    // as one stream or multiple?
    ByteBuffer byte_buffer;
    if (contents->is<ArrayObject>()) {
        auto array = contents->cast<ArrayObject>();
        for (auto& ref : *array) {
            auto bytes = TRY(document.resolve_to<StreamObject>(ref))->bytes();
            byte_buffer.append(bytes.data(), bytes.size());
        }
    } else {
        auto bytes = contents->cast<StreamObject>()->bytes();
        byte_buffer.append(bytes.data(), bytes.size());
    }
    return byte_buffer;
}

}
