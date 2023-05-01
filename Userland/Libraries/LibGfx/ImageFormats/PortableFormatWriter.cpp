/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org  >
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PortableFormatWriter.h"
#include <AK/Stream.h>

namespace Gfx {

ErrorOr<void> PortableFormatWriter::encode(Stream& output, Bitmap const& bitmap, Options options)
{
    // FIXME: Add support for PBM and PGM

    TRY(add_header(output, options, bitmap.width(), bitmap.height(), 255));
    TRY(add_pixels(output, options, bitmap));

    return {};
}

ErrorOr<void> PortableFormatWriter::add_header(Stream& output, Options const& options, u32 width, u32 height, u32 maximal_value)
{
    TRY(output.write_formatted("P{}\n", options.format == Options::Format::ASCII ? "3"sv : "6"sv));
    TRY(output.write_formatted("# {}\n", options.comment));
    TRY(output.write_formatted("{} {}\n", width, height));
    TRY(output.write_formatted("{}\n", maximal_value));

    return {};
}

ErrorOr<void> PortableFormatWriter::add_pixels(Stream& output, Options const& options, Bitmap const& bitmap)
{
    for (int i = 0; i < bitmap.height(); ++i) {
        for (int j = 0; j < bitmap.width(); ++j) {
            auto color = bitmap.get_pixel(j, i);
            if (options.format == Options::Format::ASCII) {
                TRY(output.write_formatted("{} {} {}\t", color.red(), color.green(), color.blue()));
            } else {
                TRY(output.write_value(color.red()));
                TRY(output.write_value(color.green()));
                TRY(output.write_value(color.blue()));
            }
        }
        if (options.format == Options::Format::ASCII)
            TRY(output.write_value('\n'));
    }

    return {};
}

}
