/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibGfx/ImageFormats/PortableImageMapLoader.h>

namespace Gfx {

struct PPM {
    static constexpr auto ascii_magic_number = '3';
    static constexpr auto binary_magic_number = '6';
    static constexpr StringView image_type = "PPM"sv;
    u16 max_val { 0 };
};

using PPMLoadingContext = PortableImageMapLoadingContext<PPM>;
using PPMImageDecoderPlugin = PortableImageDecoderPlugin<PPMLoadingContext>;

ErrorOr<void> read_image_data(PPMLoadingContext& context);
}
