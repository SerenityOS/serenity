/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <LibGfx/Size.h>

namespace Gfx::JPEG2000 {

template<class T>
struct Span2D {
    Span<T> data;
    IntSize size;
    int pitch;
};

}
