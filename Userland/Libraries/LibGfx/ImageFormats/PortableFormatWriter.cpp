/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org  >
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PortableFormatWriter.h"
#include <AK/String.h>

namespace Gfx {

ErrorOr<ByteBuffer> PortableFormatWriter::encode(Bitmap const& bitmap, Options options)
{
    ByteBuffer buffer;

    // FIXME: Add support for PBM and PGM

    TRY(add_header(buffer, options, bitmap.width(), bitmap.height(), 255));
    TRY(add_pixels(buffer, options, bitmap));

    return buffer;
}

ErrorOr<void> PortableFormatWriter::add_header(ByteBuffer& buffer, Options const& options, u32 width, u32 height, u32 maximal_value)
{
    TRY(buffer.try_append(TRY(String::formatted("P{}\n", options.format == Options::Format::ASCII ? "3"sv : "6"sv)).bytes()));
    TRY(buffer.try_append(TRY(String::formatted("# {}\n", options.comment)).bytes()));
    TRY(buffer.try_append(TRY(String::formatted("{} {}\n", width, height)).bytes()));
    TRY(buffer.try_append(TRY(String::formatted("{}\n", maximal_value)).bytes()));

    return {};
}

ErrorOr<void> PortableFormatWriter::add_pixels(ByteBuffer& buffer, Options const& options, Bitmap const& bitmap)
{
    for (int i = 0; i < bitmap.height(); ++i) {
        for (int j = 0; j < bitmap.width(); ++j) {
            auto color = bitmap.get_pixel(j, i);
            if (options.format == Options::Format::ASCII) {
                TRY(buffer.try_append(TRY(String::formatted("{} {} {}\t", color.red(), color.green(), color.blue())).bytes()));
            } else {
                TRY(buffer.try_append(color.red()));
                TRY(buffer.try_append(color.green()));
                TRY(buffer.try_append(color.blue()));
            }
        }
        if (options.format == Options::Format::ASCII)
            TRY(buffer.try_append('\n'));
    }

    return {};
}

}
