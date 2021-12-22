/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSoftGPU/ImageFormat.h>

namespace SoftGPU {

struct ImageDataLayout final {
    ImageFormat format;
    size_t column_stride;
    size_t row_stride;
    size_t depth_stride;
};

}
