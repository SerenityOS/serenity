/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Math.h>
#include <AK/Types.h>

namespace Gfx {

// Implementation details for the "Quite OK Image" format (v1.0).
// https://qoiformat.org/qoi-specification.pdf

static constexpr ReadonlyByteArray<4> QOI_MAGIC { 'q', 'o', 'i', 'f' };
static constexpr u8 QOI_OP_RGB = 0b11111110;
static constexpr u8 QOI_OP_RGBA = 0b11111111;
static constexpr u8 QOI_OP_INDEX = 0b00000000;
static constexpr u8 QOI_OP_DIFF = 0b01000000;
static constexpr u8 QOI_OP_LUMA = 0b10000000;
static constexpr u8 QOI_OP_RUN = 0b11000000;
// Note that the run-lengths 63 and 64 encoded as (b111110 and b111111) are illegal
// as they are occupied by the QOI_OP_RGB and QOI_OP_RGBA tags.
static constexpr u8 QOI_RUN_MIN = 1;
static constexpr u8 QOI_RUN_MAX = 62;
static constexpr u8 QOI_MASK_2 = 0b11000000;
static constexpr ReadonlyByteArray<8> QOI_END_MARKER { 0, 0, 0, 0, 0, 0, 0, 1 };

struct [[gnu::packed]] QOIHeader {
    u8 magic[QOI_MAGIC.size()];
    u32 width;
    u32 height;
    u8 channels;
    u8 colorspace;
};

ALWAYS_INLINE static constexpr bool qoi_is_valid_run(u8 run)
{
    return AK::is_in_bounds(run, QOI_RUN_MIN, QOI_RUN_MAX);
}

}
