/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGfx/Forward.h>

namespace Gfx {

struct TIFFEncoderOptions {
    Optional<ReadonlyBytes> icc_data;
};

class TIFFWriter {
public:
    using Options = TIFFEncoderOptions;

    static ErrorOr<void> encode(Stream&, Bitmap const&, Options const& = {});
    static ErrorOr<void> encode(Stream&, CMYKBitmap const&, Options const& = {});

private:
    TIFFWriter() = delete;
};

}
