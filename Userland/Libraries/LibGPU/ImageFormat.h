/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace GPU {

enum class ImageFormat {
    RGB565,
    RGB888,
    BGR888,
    RGBA8888,
    BGRA8888,
    L8,
    L8A8,
};

static constexpr size_t element_size(ImageFormat format)
{
    switch (format) {
    case ImageFormat::L8:
        return 1;
    case ImageFormat::RGB565:
    case ImageFormat::L8A8:
        return 2;
    case ImageFormat::RGB888:
    case ImageFormat::BGR888:
        return 3;
    case ImageFormat::RGBA8888:
    case ImageFormat::BGRA8888:
        return 4;
    default:
        VERIFY_NOT_REACHED();
    }
}

}
