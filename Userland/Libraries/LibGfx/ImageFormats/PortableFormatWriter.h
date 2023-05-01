/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <LibGfx/Bitmap.h>

namespace Gfx {

// This is not a nested struct to work around https://llvm.org/PR36684
struct PortableFormatWriterOptions {
    enum class Format {
        ASCII,
        Raw,
    };

    Format format = Format::Raw;
    StringView comment = "Generated with SerenityOS - LibGfx."sv;
};

class PortableFormatWriter {
public:
    using Options = PortableFormatWriterOptions;

    static ErrorOr<void> encode(Stream&, Bitmap const&, Options options = Options {});

private:
    PortableFormatWriter() = delete;

    static ErrorOr<void> add_header(Stream&, Options const& options, u32 width, u32 height, u32 max_value);
    static ErrorOr<void> add_pixels(Stream&, Options const& options, Bitmap const&);
};

}
