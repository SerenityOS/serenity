/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "JPEGXLBoxes.h"

namespace Gfx::ISOBMFF {

ErrorOr<void> JPEGXLSignatureBox::read_from_stream(ConstrainedStream& stream)
{
    // FIXME: Make the decoder check the signature.
    TRY(stream.discard(4));
    return {};
}

void JPEGXLSignatureBox::dump(String const& prepend) const
{
    Box::dump(prepend);
}

ErrorOr<void> JPEGXLLevelBox::read_from_stream(ConstrainedStream& stream)
{
    level = TRY(stream.read_value<u8>());
    return {};
}

void JPEGXLLevelBox::dump(String const& prepend) const
{
    Box::dump(prepend);
    outln("{}- level = {}", prepend, level);
}

ErrorOr<void> JPEGXLCodestreamBox::read_from_stream(ConstrainedStream& stream)
{
    // FIXME: Prevent the copy.
    TRY(codestream.try_resize(stream.remaining()));
    TRY(stream.read_until_filled(codestream.span()));
    return {};
}

void JPEGXLCodestreamBox::dump(String const& prepend) const
{
    Box::dump(prepend);
    outln("{}- size = {}", prepend, codestream.size());
}

}
