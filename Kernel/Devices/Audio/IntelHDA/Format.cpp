/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Format.h"
#include <AK/Array.h>

namespace Kernel::Audio::IntelHDA {

// 3.3.41: Input/Output/Bidirectional Stream Descriptor Format
// 3.7.1: Stream Format Structure
struct SampleRateParameters {
    u32 sample_rate;
    u8 base;
    u8 multiple;
    u8 divisor;
};
static constexpr Array<SampleRateParameters, 15> sample_rate_parameters { {
    // clang-format off
    { 6'000,   0b0, 0b000, 0b111 },
    { 8'000,   0b0, 0b000, 0b101 },
    { 9'600,   0b0, 0b000, 0b100 },
    { 11'025,  0b1, 0b000, 0b011 },
    { 16'000,  0b0, 0b000, 0b010 },
    { 22'050,  0b1, 0b000, 0b001 },
    { 24'000,  0b0, 0b000, 0b001 },
    { 32'000,  0b0, 0b001, 0b010 },
    { 44'100,  0b1, 0b000, 0b000 },
    { 48'000,  0b0, 0b000, 0b000 },
    { 88'200,  0b1, 0b001, 0b000 },
    { 96'000,  0b0, 0b001, 0b000 },
    { 144'000, 0b0, 0b010, 0b000 },
    { 176'400, 0b1, 0b011, 0b000 },
    { 192'000, 0b0, 0b011, 0b000 },
    // clang-format on
} };

struct PcmBitsParameters {
    u8 pcm_bits;
    u8 encoding;
};
static constexpr Array<PcmBitsParameters, 5> pcm_bits_parameters { {
    // clang-format off
    { 8,  0b000 },
    { 16, 0b001 },
    { 20, 0b010 },
    { 24, 0b011 },
    { 32, 0b100 },
    // clang-format on
} };

ErrorOr<u16> encode_format(FormatParameters format)
{
    // 3.3.41: Input/Output/Bidirectional Stream Descriptor Format
    // 3.7.1: Stream Format Structure

    // Stream type
    // NOTE: we only support PCM streams
    auto is_pcm = true;

    // Sample rate parameters
    Optional<SampleRateParameters> selected_sample_rate {};
    for (auto sample_rate_parameter : sample_rate_parameters) {
        if (sample_rate_parameter.sample_rate == format.sample_rate) {
            selected_sample_rate = sample_rate_parameter;
            break;
        }
    }
    if (!selected_sample_rate.has_value())
        return ENOTSUP;

    // Bit size
    Optional<PcmBitsParameters> selected_bit_rate {};
    for (auto pcm_bits_parameter : pcm_bits_parameters) {
        if (pcm_bits_parameter.pcm_bits == format.pcm_bits) {
            selected_bit_rate = pcm_bits_parameter;
            break;
        }
    }
    if (!selected_bit_rate.has_value())
        return ENOTSUP;

    // Number of channels
    if (format.number_of_channels < 1 || format.number_of_channels > 16)
        return ENOTSUP;

    // Construct stream format
    return ((is_pcm ? 0 : 1) << 15)
        | ((selected_sample_rate->base & 0x1) << 14)
        | ((selected_sample_rate->multiple & 0x7) << 11)
        | ((selected_sample_rate->divisor & 0x7) << 8)
        | ((selected_bit_rate->encoding & 0x7) << 4)
        | ((format.number_of_channels - 1) & 0xf);
}

ErrorOr<FormatParameters> decode_format(u16 format)
{
    // 3.3.41: Input/Output/Bidirectional Stream Descriptor Format
    // 3.7.1: Stream Format Structure

    // Sample rate
    u8 sample_rate_base = (format >> 14) & 0x1;
    u8 sample_rate_multiple = (format >> 11) & 0x7;
    u8 sample_rate_divisor = (format >> 8) & 0x7;
    Optional<SampleRateParameters> found_sample_rate {};
    for (auto sample_rate_parameter : sample_rate_parameters) {
        if (sample_rate_parameter.base == sample_rate_base
            && sample_rate_parameter.multiple == sample_rate_multiple
            && sample_rate_parameter.divisor == sample_rate_divisor) {
            found_sample_rate = sample_rate_parameter;
            break;
        }
    }

    // PCM bits
    u8 pcm_bits = (format >> 4) & 0x7;
    Optional<PcmBitsParameters> found_pcm_bits {};
    for (auto pcm_bits_parameter : pcm_bits_parameters) {
        if (pcm_bits_parameter.encoding == pcm_bits) {
            found_pcm_bits = pcm_bits_parameter;
            break;
        }
    }

    // Number of channels
    u8 number_of_channels = (format & 0xf) + 1;

    if (!found_sample_rate.has_value() || !found_pcm_bits.has_value())
        return EINVAL;

    return FormatParameters {
        .sample_rate = found_sample_rate.release_value().sample_rate,
        .pcm_bits = found_pcm_bits.release_value().pcm_bits,
        .number_of_channels = number_of_channels,
    };
}

}
