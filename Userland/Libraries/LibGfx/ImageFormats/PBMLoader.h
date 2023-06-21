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

struct PBM {
    static constexpr auto ascii_magic_number = '1';
    static constexpr auto binary_magic_number = '4';
    static constexpr StringView image_type = "PBM"sv;
};

using PBMLoadingContext = PortableImageMapLoadingContext<PBM>;
using PBMImageDecoderPlugin = PortableImageDecoderPlugin<PBMLoadingContext>;

ErrorOr<void> read_image_data(PBMLoadingContext& context);
}
