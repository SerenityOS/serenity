/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Forward.h>

namespace Gfx {

class QOIWriter final {
public:
    static ErrorOr<ByteBuffer> encode(Gfx::Bitmap const&);
};

}
