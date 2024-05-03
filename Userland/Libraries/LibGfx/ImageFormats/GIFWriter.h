/*
 * Copyright (c) 2024, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGfx/Forward.h>

namespace Gfx {

// Specified at: https://www.w3.org/Graphics/GIF/spec-gif89a.txt

class GIFWriter {
public:
    static ErrorOr<void> encode(Stream&, Bitmap const&);
};

}
