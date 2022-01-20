/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGfx/BitmapFont.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
#ifdef __serenity__
    TRY(Core::System::pledge("stdio rpath"));
#endif

    StringView font_path;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Dump information about a font");
    args_parser.add_positional_argument(font_path, "Font path", "path", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    auto maybe_font = Gfx::BitmapFont::load_from_file(font_path);
    if (maybe_font.is_null()) {
        auto error = String::formatted("Error: font {} could not be loaded.", font_path);
        return Error::from_string_literal(error);
    }
    auto font = maybe_font.release_nonnull();

    size_t total_glyphs = font->glyph_count();
    size_t treated_glyphs = 0;
    u32 current_codepoint = 0;
    Vector<u32> defined_glyphs;
    TRY(defined_glyphs.try_ensure_capacity(total_glyphs));

    // FIXME: Is this the highest possible number of a code point?
    while (treated_glyphs < total_glyphs && current_codepoint <= 0x10FFFF) {
        if (font->contains_glyph(current_codepoint)) {
            TRY(defined_glyphs.try_append(current_codepoint));
            ++treated_glyphs;
        }
        ++current_codepoint;
    }

    auto json_codepoint_list = JsonArray(defined_glyphs).to_string();
    outln(json_codepoint_list);

    return 0;
}
