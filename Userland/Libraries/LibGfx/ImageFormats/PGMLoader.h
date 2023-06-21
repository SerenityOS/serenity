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

struct PGM {
    static constexpr auto ascii_magic_number = '2';
    static constexpr auto binary_magic_number = '5';
    static constexpr StringView image_type = "PGM"sv;
    u16 max_val { 0 };
};

using PGMLoadingContext = PortableImageMapLoadingContext<PGM>;
using PGMImageDecoderPlugin = PortableImageDecoderPlugin<PGMLoadingContext>;

ErrorOr<void> read_image_data(PGMLoadingContext& context);
}
