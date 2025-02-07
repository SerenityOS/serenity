/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibGfx/ImageFormats/PortableImageMapLoader.h>

namespace Gfx {

struct PAM {
    static constexpr auto binary_magic_number = '7';
    static constexpr StringView image_type = "PAM"sv;
    u16 max_val { 0 };
    u16 depth { 0 };
    String tupl_type {};
    Optional<NonnullRefPtr<CMYKBitmap>> cmyk_bitmap {};
};

using PAMLoadingContext = PortableImageMapLoadingContext<PAM>;

template<class Context>
ErrorOr<void> read_pam_header(Context& context)
{
    // https://netpbm.sourceforge.net/doc/pam.html
    TRY(read_magic_number(context));

    Optional<u16> width;
    Optional<u16> height;
    Optional<u16> depth;
    Optional<u16> max_val;
    Optional<String> tupltype;

    while (true) {
        TRY(read_whitespace(context));

        auto const token = TRY(read_token(*context.stream));

        if (token == "ENDHDR") {
            auto newline = TRY(context.stream->template read_value<u8>());
            if (newline != '\n')
                return Error::from_string_view("PAM ENDHDR not followed by newline"sv);
            break;
        }

        TRY(read_whitespace(context));
        if (token == "WIDTH") {
            if (width.has_value())
                return Error::from_string_view("Duplicate PAM WIDTH field"sv);
            width = TRY(read_number(*context.stream));
        } else if (token == "HEIGHT") {
            if (height.has_value())
                return Error::from_string_view("Duplicate PAM HEIGHT field"sv);
            height = TRY(read_number(*context.stream));
        } else if (token == "DEPTH") {
            if (depth.has_value())
                return Error::from_string_view("Duplicate PAM DEPTH field"sv);
            depth = TRY(read_number(*context.stream));
        } else if (token == "MAXVAL") {
            if (max_val.has_value())
                return Error::from_string_view("Duplicate PAM MAXVAL field"sv);
            max_val = TRY(read_number(*context.stream));
        } else if (token == "TUPLTYPE") {
            // FIXME: tupltype should be all text until the next newline, with leading and trailing space stripped.
            // FIXME: If there are multiple TUPLTYPE lines, their values are all appended.
            tupltype = TRY(read_token(*context.stream));
        } else {
            return Error::from_string_view("Unknown PAM token"sv);
        }
    }

    if (!width.has_value() || !height.has_value() || !depth.has_value() || !max_val.has_value())
        return Error::from_string_view("Missing PAM header fields"sv);
    context.width = *width;
    context.height = *height;
    context.format_details.depth = *depth;
    context.format_details.max_val = *max_val;
    if (tupltype.has_value())
        context.format_details.tupl_type = *tupltype;

    context.state = Context::State::HeaderDecoded;

    return {};
}

using PAMImageDecoderPlugin = PortableImageDecoderPlugin<PAMLoadingContext>;

ErrorOr<void> read_image_data(PAMLoadingContext& context);
}
