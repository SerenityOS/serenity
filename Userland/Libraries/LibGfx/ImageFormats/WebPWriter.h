/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGfx/Forward.h>

namespace Gfx {

struct WebPEncoderOptions {
    Optional<ReadonlyBytes> icc_data;
};

class WebPWriter {
public:
    using Options = WebPEncoderOptions;

    // Always lossless at the moment.
    static ErrorOr<void> encode(Stream&, Bitmap const&, Options const& = {});

private:
    WebPWriter() = delete;
};

}
