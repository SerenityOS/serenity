/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "JPEG2000Boxes.h"

namespace Gfx::ISOBMFF {

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
