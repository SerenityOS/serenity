/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/ImageFormats/WebPWriter.h>

namespace Gfx {

ErrorOr<void> WebPWriter::encode(Stream&, Bitmap const&, Options const&)
{
    return Error::from_string_literal("WebP encoding not yet implemented");
}

}
