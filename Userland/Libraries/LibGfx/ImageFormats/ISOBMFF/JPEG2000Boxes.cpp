/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "JPEG2000Boxes.h"
#include <AK/Function.h>

namespace Gfx::ISOBMFF {

ErrorOr<void> JPEG2000HeaderBox::read_from_stream(BoxStream& stream)
{
    auto make_subbox = [](BoxType, BoxStream&) -> ErrorOr<Optional<NonnullOwnPtr<Box>>> {
        return OptionalNone {};
    };

    TRY(SuperBox::read_from_stream(stream, move(make_subbox)));
    return {};
}

void JPEG2000HeaderBox::dump(String const& prepend) const
{
    SuperBox::dump(prepend);
}

ErrorOr<void> JPEG2000SignatureBox::read_from_stream(BoxStream& stream)
{
    signature = TRY(stream.read_value<BigEndian<u32>>());
    return {};
}

void JPEG2000SignatureBox::dump(String const& prepend) const
{
    Box::dump(prepend);
    outln("{}- signature = {:#08x}", prepend, signature);
}

}
