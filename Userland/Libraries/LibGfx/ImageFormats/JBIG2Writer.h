/*
 * Copyright (c) 2024-2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGfx/Forward.h>
#include <LibGfx/ImageFormats/JBIG2Shared.h>

namespace Gfx {

struct JBIG2EncoderOptions {
    JBIG2::CombinationOperator default_combination_operator { JBIG2::CombinationOperator::Or };
};

class JBIG2Writer {
public:
    using Options = JBIG2EncoderOptions;

    static ErrorOr<void> encode(Stream&, Bitmap const&, Options const& = {});

private:
    JBIG2Writer() = delete;
};

}
