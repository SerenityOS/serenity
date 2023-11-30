/*
 * Copyright (c) 2023, Stephan Vedder <stephan.vedder@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>

namespace Video {

enum class CodecID : u32 {
    Unknown,
    // On2 / Google
    VP8,
    VP9,
    // MPEG
    H261,
    MPEG1,
    H262,
    H263,
    H264,
    H265,
    // AOMedia
    AV1,
    // Xiph
    Theora,
    Vorbis,
    Opus,
};

}

namespace AK {
template<>
struct Formatter<Video::CodecID> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Video::CodecID value)
    {
        StringView codec;
        switch (value) {
        case Video::CodecID::Unknown:
            codec = "Unknown"sv;
            break;
        case Video::CodecID::VP8:
            codec = "VP8"sv;
            break;
        case Video::CodecID::VP9:
            codec = "VP9"sv;
            break;
        case Video::CodecID::H261:
            codec = "H.261"sv;
            break;
        case Video::CodecID::H262:
            codec = "H.262"sv;
            break;
        case Video::CodecID::H263:
            codec = "H.263"sv;
            break;
        case Video::CodecID::H264:
            codec = "H.264"sv;
            break;
        case Video::CodecID::H265:
            codec = "H.265"sv;
            break;
        case Video::CodecID::MPEG1:
            codec = "MPEG1"sv;
            break;
        case Video::CodecID::AV1:
            codec = "AV1"sv;
            break;
        case Video::CodecID::Theora:
            codec = "Theora"sv;
            break;
        case Video::CodecID::Vorbis:
            codec = "Vorbis"sv;
            break;
        case Video::CodecID::Opus:
            codec = "Opus"sv;
            break;
        }
        return builder.put_string(codec);
    }
};
}
