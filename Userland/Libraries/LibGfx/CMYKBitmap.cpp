/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/CMYKBitmap.h>

namespace Gfx {

ErrorOr<NonnullRefPtr<CMYKBitmap>> CMYKBitmap::create_with_size(IntSize const& size)
{
    VERIFY(size.width() >= 0 && size.height() >= 0);
    auto data = TRY(ByteBuffer::create_uninitialized(size.width() * size.height() * sizeof(CMYK)));
    return adopt_ref(*new CMYKBitmap(size, move(data)));
}

}
