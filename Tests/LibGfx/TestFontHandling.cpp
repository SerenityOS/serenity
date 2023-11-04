/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibCore/ResourceImplementationFile.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Font/OpenType/Glyf.h>
#include <LibTest/TestCase.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

static void init_font_database()
{
#ifdef AK_OS_SERENITY
    auto const test_file_root = "/usr/Tests/LibGfx/test-inputs/"_string;
#else
    auto const test_file_root = "test-inputs/"_string;
#endif

    Core::ResourceImplementation::install(make<Core::ResourceImplementationFile>(test_file_root));
    Gfx::FontDatabase::the().load_all_fonts_from_uri("resource:///"sv);
}

TEST_CASE(test_fontdatabase_get_by_name)
{
    init_font_database();

    auto& font_database = Gfx::FontDatabase::the();
    auto name = "Family 12 400 0"sv;
    EXPECT(!font_database.get_by_name(name)->name().is_empty());
}

TEST_CASE(test_fontdatabase_get)
{
    init_font_database();

    auto& font_database = Gfx::FontDatabase::the();
    EXPECT(!font_database.get("Family"_fly_string, 12, 400, Gfx::FontWidth::Normal, 0)->name().is_empty());
}

TEST_CASE(test_fontdatabase_for_each_font)
{
    init_font_database();

    auto& font_database = Gfx::FontDatabase::the();
    font_database.for_each_font([&](Gfx::Font const& font) {
        EXPECT(!font.name().is_empty());
        EXPECT(!font.qualified_name().is_empty());
        EXPECT(!font.family().is_empty());
        EXPECT(font.glyph_count() > 0);
    });
}

TEST_CASE(test_clone)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = MUST(Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256));

    auto new_font = font->clone();
    EXPECT(!new_font->name().is_empty());
    EXPECT(!new_font->qualified_name().is_empty());
    EXPECT(!new_font->family().is_empty());
    EXPECT(new_font->glyph_count() > 0);
}

TEST_CASE(test_set_name)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = MUST(Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256));

    auto name = "my newly created font"_string;
    font->set_name(name);

    EXPECT(!font->name().is_empty());
    EXPECT(font->name().contains(name));
}

TEST_CASE(test_set_family)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = MUST(Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256));

    auto family = "my newly created font family"_string;
    font->set_family(family);

    EXPECT(!font->family().is_empty());
    EXPECT(font->family().contains(family));
}

TEST_CASE(test_set_glyph_width)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = MUST(Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256));

    size_t ch = 123;
    font->set_glyph_width(ch, glyph_width);

    EXPECT(font->glyph_width(ch) == glyph_width);
}

TEST_CASE(test_set_glyph_spacing)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = MUST(Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256));

    u8 glyph_spacing = 8;
    font->set_glyph_spacing(glyph_spacing);

    EXPECT(font->glyph_spacing() == glyph_spacing);
}

TEST_CASE(test_width)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = MUST(Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256));

    EXPECT(font->width("A"sv) == glyph_width);
}

TEST_CASE(test_glyph_or_emoji_width)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = MUST(Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256));

    Utf8View view { " "sv };
    auto it = view.begin();

    EXPECT(font->glyph_or_emoji_width(it));
}

TEST_CASE(test_load_from_uri)
{
    init_font_database();
    auto font = Gfx::BitmapFont::load_from_uri("resource://TestFont.font"sv);
    EXPECT(!font->name().is_empty());
}

TEST_CASE(test_write_to_file)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = MUST(Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256));

    char path[] = "/tmp/new.font.XXXXXX";
    EXPECT(mkstemp(path) != -1);
    TRY_OR_FAIL(font->write_to_file(path));
    unlink(path);
}

TEST_CASE(test_character_set_masking)
{
    init_font_database();
    auto font = TRY_OR_FAIL(Gfx::BitmapFont::try_load_from_uri("resource://TestFont.font"sv));

    auto unmasked_font = TRY_OR_FAIL(font->unmasked_character_set());
    EXPECT(unmasked_font->glyph_index(0x0041).value() == 0x0041);
    EXPECT(unmasked_font->glyph_index(0x0100).value() == 0x0100);
    EXPECT(unmasked_font->glyph_index(0xFFFD).value() == 0xFFFD);

    auto masked_font = TRY_OR_FAIL(unmasked_font->masked_character_set());
    EXPECT(masked_font->glyph_index(0x0041).value() == 0x0041);
    EXPECT(!masked_font->glyph_index(0x0100).has_value());
    EXPECT(masked_font->glyph_index(0xFFFD).value() == 0x1FD);
}

TEST_CASE(resolve_glyph_path_containing_single_off_curve_point)
{
    Vector<u8> glyph_data {
        0, 5, 0, 205, 255, 51, 7, 51, 6, 225, 0, 3, 0, 6, 0, 9, 0, 12, 0, 15, 0, 31, 64, 13, 13, 2, 15, 5, 7, 2, 8, 5, 10, 3, 0,
        5, 3, 0, 47, 47, 51, 17, 51, 17, 51, 17, 51, 17, 51, 17, 51, 48, 49, 19, 33, 17, 33, 1, 33, 1, 1, 17, 1, 1, 33, 9, 3,
        205, 6, 102, 249, 154, 5, 184, 250, 248, 2, 133, 2, 199, 253, 125, 253, 57, 5, 4, 253, 127, 253, 53, 2, 133, 253,
        123, 6, 225, 248, 82, 7, 68, 252, 231, 252, 145, 6, 50, 252, 231, 252, 149, 3, 23, 253, 57, 3, 27, 3, 29, 0, 1, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 177, 2, 81, 43, 48, 49, 48, 0
    };
    OpenType::Glyf glyf(glyph_data.span());
    auto glyph = glyf.glyph(118);
    EXPECT(glyph.has_value());
    EXPECT_NO_CRASH("resolving the path of glyph containing single off-curve point should not crash", [&] {
        Gfx::Path path;
        (void)glyph->append_path(path, 0, 0, 1, 1, [&](u16) -> Optional<OpenType::Glyf::Glyph> { VERIFY_NOT_REACHED(); });
        return Test::Crash::Failure::DidNotCrash;
    });
}
