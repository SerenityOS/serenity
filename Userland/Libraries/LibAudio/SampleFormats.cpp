/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleFormats.h"

namespace Audio {

u16 pcm_bits_per_sample(PcmSampleFormat format)
{
    switch (format) {
    case Uint8:
        return 8;
    case Int16:
        return 16;
    case Int24:
        return 24;
    case Int32:
    case Float32:
        return 32;
    case Float64:
        return 64;
    default:
        VERIFY_NOT_REACHED();
    }
}

String sample_format_name(PcmSampleFormat format)
{
    bool is_float = format == Float32 || format == Float64;
    return String::formatted("PCM {}bit {}", pcm_bits_per_sample(format), is_float ? "Float" : "LE");
}

}
