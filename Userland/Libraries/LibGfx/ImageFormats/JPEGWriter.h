/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGfx/Forward.h>

namespace Gfx {

struct JPEGEncoderOptions {
    enum class UseDeringing : u8 {
        Yes,
        No,
    };

    Optional<ReadonlyBytes> icc_data;
    u8 quality { 75 };
    UseDeringing use_deringing { UseDeringing::Yes };
};

class JPEGWriter {
public:
    using Options = JPEGEncoderOptions;

    static ErrorOr<void> encode(Stream&, Bitmap const&, Options const& = {});
    static ErrorOr<void> encode(Stream&, CMYKBitmap const&, Options const& = {});

private:
    JPEGWriter() = delete;
};

}
