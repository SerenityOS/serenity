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
    // Table 3.27 Entries in a page object on Contents:
    // "If this entry is absent, the page is empty. [...]"
    if (contents.is_null())
        return ByteBuffer {};

    // "The value may be either a single stream or an array of streams. If the value
    //  is an array, the effect is as if all the streams in the array were concatenated,
    //  in order, to form a single stream. The division between streams may occur only at
    //  the boundaries between lexical tokens"
    if (contents->is<StreamObject>())
        return TRY(ByteBuffer::copy(contents->cast<StreamObject>()->bytes()));

    // If one stream ends with (say) a `Q` and the next starts with `q`, that should be
    // two distinct tokens. Insert spaces between stream contents to ensure that.
    ByteBuffer byte_buffer;
    for (auto& ref : *contents->cast<ArrayObject>()) {
        TRY(byte_buffer.try_append(TRY(document.resolve_to<StreamObject>(ref))->bytes()));
        TRY(byte_buffer.try_append(' '));
    }
    return byte_buffer;
}

}
