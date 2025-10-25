/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/ImageFormats/TIFFWriter.h>

namespace Gfx {

ErrorOr<void> TIFFWriter::encode(Stream&, Bitmap const&, Options const&)
{
    return Error::from_string_literal("TIFF encoding not yet implemented");
}

}
