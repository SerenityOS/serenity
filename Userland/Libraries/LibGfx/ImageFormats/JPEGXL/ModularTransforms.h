/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitStream.h>
#include <AK/Error.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibGfx/ImageFormats/JPEGXL/Channel.h>
#include <LibGfx/ImageFormats/JPEGXL/SelfCorrectingPredictor.h>

namespace Gfx::JPEGXL {

/// H.6 - Transformations

// H.6.2.1 - Parameters
struct SqueezeParams {
    bool horizontal {};
    bool in_place {};
    u32 begin_c {};
    u32 num_c {};
};

ErrorOr<SqueezeParams> read_squeeze_params(LittleEndianInputBitStream& stream);

// H.6.1 - General
struct TransformInfo {
    enum class TransformId {
        kRCT = 0,
        kPalette = 1,
        kSqueeze = 2,
    };

    TransformId tr {};
    u32 begin_c {};
    u32 rct_type {};

    u32 num_c {};
    u32 nb_colours {};
    u32 nb_deltas {};
    u8 d_pred {};

    Vector<SqueezeParams> sp {};
};

ErrorOr<TransformInfo> read_transform_info(LittleEndianInputBitStream& stream);

ErrorOr<void> apply_transformation(Vector<Channel>&, TransformInfo const&, u32 bit_depth, WPHeader const& wp_header);
}
