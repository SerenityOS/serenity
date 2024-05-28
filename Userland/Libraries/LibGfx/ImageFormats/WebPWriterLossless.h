/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGfx/Forward.h>

namespace Gfx {

ErrorOr<ByteBuffer> compress_VP8L_image_data(Bitmap const&, bool& is_fully_opaque);

}
