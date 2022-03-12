/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibGfx/Bitmap.h>

namespace Gfx {

template<class TFormatDetails>
struct PortableImageMapLoadingContext {
    using FormatDetails = TFormatDetails;

    enum class Type {
        Unknown,
        ASCII,
        RAWBITS
    };

    enum class State {
        NotDecoded = 0,
        Error,
        MagicNumber,
        Width,
        Height,
        Maxval,
        Bitmap,
        Decoded
    };

    Type type { Type::Unknown };
    State state { State::NotDecoded };
    u8 const* data { nullptr };
    size_t data_size { 0 };
    size_t width { 0 };
    size_t height { 0 };
    FormatDetails format_details {};
    RefPtr<Gfx::Bitmap> bitmap;
};

}
