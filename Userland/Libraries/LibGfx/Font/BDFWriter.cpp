/*
 * Copyright (c) 2022, Marco Rebhan <me@dblsaiko.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BDFWriter.h"
#include "FontStyleMapping.h"
#include <LibCore/FileStream.h>

namespace Gfx {

static ErrorOr<void> vwrite(Core::Stream::Stream& stream, StringView fmtstr, AK::TypeErasedFormatParams& params)
{
    String s = String::vformatted(fmtstr, params);
    return stream.write_all(s.bytes());
}

template<typename... Parameters>
static ErrorOr<void> write(Core::Stream::Stream& stream, CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    AK::VariadicFormatParams variadic_format_parameters { parameters... };
    return vwrite(stream, fmtstr.view(), variadic_format_parameters);
}

template<typename... Parameters>
static ErrorOr<void> writeln(Core::Stream::Stream& stream, CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    AK::VariadicFormatParams variadic_format_parameters { parameters... };
    TRY(vwrite(stream, fmtstr.view(), variadic_format_parameters));
    return write(stream, "\n");
}

static ErrorOr<void> writeln(Core::Stream::Stream& stream)
{
    return write(stream, "\n");
}

static ErrorOr<void> write_font_header(Core::Stream::Stream& stream, BitmapFont const& font);

static ErrorOr<void> write_glyph_data(Core::Stream::Stream& stream, BitmapFont const& font, size_t index);

ErrorOr<void> write_bdf(String const& path, BitmapFont const& font)
{
    auto stream = TRY(Core::Stream::File::open(path, Core::Stream::OpenMode::Write | Core::Stream::OpenMode::Truncate));
    return write_bdf(*stream, font);
}

ErrorOr<void> write_bdf(Core::Stream::Stream& stream, BitmapFont const& font)
{
    TRY(writeln(stream, "STARTFONT 2.1"));
    TRY(write_font_header(stream, font));

    size_t actual_chars = 0;

    for (size_t i = 0; i < font.glyph_count(); i += 1) {
        if (font.glyph_width_at(i) == 0)
            continue; // unset

        Glyph const& g = font.glyph_at(i);

        if (!g.is_glyph_bitmap())
            continue; // this is a color one, don't export it for now

        actual_chars += 1;
    }

    TRY(writeln(stream, "CHARS {}", actual_chars));

    for (size_t i = 0; i < font.glyph_count(); i += 1) {
        if (font.glyph_width_at(i) == 0)
            continue; // unset

        Glyph const& g = font.glyph_at(i);

        if (!g.is_glyph_bitmap())
            continue; // this is a color one, don't export it for now

        TRY(write_glyph_data(stream, font, i));
    }

    TRY(writeln(stream, "ENDFONT"));

    return {};
}

static ErrorOr<void> write_font_header(Core::Stream::Stream& stream, BitmapFont const& font)
{
    TRY(writeln(stream, "COMMENT {}", font.human_readable_name()));

    char const* foundry = "SerenityOS";
    String const& family = font.family();
    StringView const& weight = weight_to_name(font.weight());
    int relative_weight = font.weight() / 10;

    // The RELATIVE_WEIGHT field is defined as ranged 10 - 90, however the max
    // defined weight in SerenityOS is 950, so shrink range 80-95 into 80-90.
    if (relative_weight > 80) {
        relative_weight = 80 + ((relative_weight - 80) / 3 * 2);
    }

    char const* slant_names[4] = { "R", "I", "O", "RI" };
    StringView const& slant = font.slope() < 5 ? slant_names[font.slope()] : "OT";
    char const* width_name = "Normal";
    char const* additional_style = "";
    int pixel_size = font.preferred_line_height();
    int point_size = font.preferred_line_height() * 10;
    int x_res = 72;
    int y_res = 72;
    char const* spacing = font.is_fixed_width() ? "C" : "P";
    int average_width = (font.max_glyph_width() + font.min_glyph_width()) * 10 / 2;
    char const* charset_registry = "ISO10646"; // Unicode
    char const* charset_encoding = "1";

    int line_gap = font.preferred_line_height() - font.glyph_height();

    int descent = font.glyph_height() - font.baseline();
    int yoff = 1 - descent;

    TRY(writeln(
        stream,
        "FONT -{}-{}-{}-{}-{}-{}-{}-{}-{}-{}-{}-{}-{}-{}",
        foundry,
        family,
        weight,
        slant,
        width_name,
        additional_style,
        pixel_size,
        point_size,
        x_res,
        y_res,
        spacing,
        average_width,
        charset_registry,
        charset_encoding));
    TRY(writeln(stream, "SIZE {} {} {}", font.presentation_size(), x_res, y_res));
    TRY(writeln(stream, "FONTBOUNDINGBOX {} {} {} {}", font.max_glyph_width(), font.glyph_height(), 0, yoff));

    // See https://www.x.org/releases/X11R7.6-RC1/doc/xorg-docs/specs/XLFD/xlfd.html
    // for more information on these fields.
    TRY(writeln(stream, "STARTPROPERTIES {}", 19));
    TRY(writeln(stream, "FAMILY_NAME \"{}\"", family));
    TRY(writeln(stream, "FOUNDRY \"{}\"", foundry));
    TRY(writeln(stream, "SETWIDTH_NAME \"{}\"", width_name));
    TRY(writeln(stream, "ADD_STYLE_NAME \"{}\"", additional_style));
    TRY(writeln(stream, "WEIGHT_NAME \"{}\"", weight));
    TRY(writeln(stream, "RELATIVE_WEIGHT {}", relative_weight));
    TRY(writeln(stream, "SLANT \"{}\"", slant));
    TRY(writeln(stream, "PIXEL_SIZE {}", pixel_size));
    TRY(writeln(stream, "POINT_SIZE {}", point_size));
    TRY(writeln(stream, "RESOLUTION_X {}", x_res));
    TRY(writeln(stream, "RESOLUTION_Y {}", y_res));
    TRY(writeln(stream, "SPACING \"{}\"", spacing));
    TRY(writeln(stream, "AVERAGE_WIDTH {}", average_width));
    TRY(writeln(stream, "CHARSET_REGISTRY \"{}\"", charset_registry));
    TRY(writeln(stream, "CHARSET_ENCODING \"{}\"", charset_encoding));
    TRY(writeln(stream, "FONT_ASCENT {}", font.baseline() + line_gap));
    TRY(writeln(stream, "FONT_DESCENT {}", descent));
    TRY(writeln(stream, "X_HEIGHT {}", font.x_height()));
    TRY(writeln(stream, "DEFAULT_CHAR {}", 65533));
    TRY(writeln(stream, "ENDPROPERTIES"));

    return {};
}

static ErrorOr<void> write_glyph_data(Core::Stream::Stream& stream, BitmapFont const& font, size_t index)
{
    int descent = font.glyph_height() - font.baseline();
    int yoff = 1 - descent;

    u32 code_point = font.index_to_codepoint(index);
    TRY(writeln(stream, "STARTCHAR U+{:04X}", code_point));
    TRY(writeln(stream, "ENCODING {}", code_point));

    u8 width = font.is_fixed_width() ? font.glyph_fixed_width() : font.glyph_width_at(index);

    TRY(writeln(stream, "SWIDTH {} {}", (width + font.glyph_spacing()) * 1000 / (int)font.point_size(), 0));
    TRY(writeln(stream, "DWIDTH {} {}", (width + font.glyph_spacing()), 0));

    TRY(writeln(stream, "BBX {} {} {} {}", width, font.glyph_height(), 0, yoff));
    TRY(writeln(stream, "BITMAP"));

    Glyph const& g = font.glyph_at(index);

    for (int y = 0; y < font.glyph_height(); y += 1) {
        for (int x_chunk = 0; x_chunk < (width + 7) / 8; x_chunk += 1) {
            u8 data = 0;
            auto const& bitmap = g.glyph_bitmap();

            for (int sub_chunk = 0; sub_chunk < 8; sub_chunk += 1) {
                data = data << 1 | (bitmap.bit_at(x_chunk * 8 + sub_chunk, y) ? 1 : 0);
            }

            TRY(write(stream, "{:02X}", data));
        }

        TRY(writeln(stream));
    }

    TRY(writeln(stream, "ENDCHAR"));
    return {};
}

}
