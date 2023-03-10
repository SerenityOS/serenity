/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Types.h>

namespace Kernel::Audio::IntelHDA {

struct FormatParameters {
    u32 sample_rate;
    u8 pcm_bits;
    u8 number_of_channels;
};

ErrorOr<u16> encode_format(FormatParameters format);
ErrorOr<FormatParameters> decode_format(u16 format);

}
