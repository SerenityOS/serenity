/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGPU/ImageFormat.h>

namespace GPU {

struct ImageDataLayout final {
    GPU::ImageFormat format;
    size_t column_stride;
    size_t row_stride;
    size_t depth_stride;
};

}
