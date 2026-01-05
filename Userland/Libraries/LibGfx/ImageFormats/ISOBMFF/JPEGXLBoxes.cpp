/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "JPEGXLBoxes.h"

namespace Gfx::ISOBMFF {

ErrorOr<void> JPEGXLSignatureBox::read_from_stream(ConstrainedStream& stream)
{
    signature = TRY(stream.read_value<BigEndian<u32>>());
    return {};
}

void JPEGXLSignatureBox::dump(String const& prepend) const
{
    Box::dump(prepend);
    outln("{}- signature = {:#08x}", prepend, signature);
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

ErrorOr<void> JPEGXLPartialCodestreamBox::read_from_stream(ConstrainedStream& stream)
{
    part_index = TRY(stream.read_value<BigEndian<u32>>());

    // FIXME: Prevent the copy.
    TRY(codestream.try_resize(stream.remaining()));
    TRY(stream.read_until_filled(codestream.span()));
    return {};
}

void JPEGXLPartialCodestreamBox::dump(String const& prepend) const
{
    Box::dump(prepend);
    outln("{}- index = {}{}", prepend, index(), is_last() ? " (last)" : "");
    outln("{}- size = {}", prepend, codestream.size());
}

}
