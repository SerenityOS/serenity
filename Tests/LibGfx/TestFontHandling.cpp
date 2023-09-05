/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibTest/TestCase.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#ifdef AK_OS_SERENITY
#    define TEST_INPUT(x) ("/usr/Tests/LibGfx/test-inputs/" x)
#else
#    define TEST_INPUT(x) ("test-inputs/" x)
#endif

TEST_CASE(test_fontdatabase_get_by_name)
{
    Gfx::FontDatabase::set_default_fonts_lookup_path(TEST_INPUT(""));

    auto name = "Family 12 400 0"sv;
    auto& font_database = Gfx::FontDatabase::the();
    EXPECT(!font_database.get_by_name(name)->name().is_empty());
}

TEST_CASE(test_fontdatabase_get)
{
    Gfx::FontDatabase::set_default_fonts_lookup_path(TEST_INPUT(""));
    auto& font_database = Gfx::FontDatabase::the();
    EXPECT(!font_database.get("Family"_fly_string, 12, 400, Gfx::FontWidth::Normal, 0)->name().is_empty());
}

TEST_CASE(test_fontdatabase_for_each_font)
{
    Gfx::FontDatabase::set_default_fonts_lookup_path(TEST_INPUT(""));

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
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

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
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

    auto name = "my newly created font"_string;
    font->set_name(name);

    EXPECT(!font->name().is_empty());
    EXPECT(font->name().contains(name));
}

TEST_CASE(test_set_family)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

    auto family = "my newly created font family"_string;
    font->set_family(family);

    EXPECT(!font->family().is_empty());
    EXPECT(font->family().contains(family));
}

TEST_CASE(test_set_glyph_width)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

    size_t ch = 123;
    font->set_glyph_width(ch, glyph_width);

    EXPECT(font->glyph_width(ch) == glyph_width);
}

TEST_CASE(test_set_glyph_spacing)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

    u8 glyph_spacing = 8;
    font->set_glyph_spacing(glyph_spacing);

    EXPECT(font->glyph_spacing() == glyph_spacing);
}

TEST_CASE(test_width)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

    EXPECT(font->width("A"sv) == glyph_width);
}

TEST_CASE(test_glyph_or_emoji_width)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

    Utf8View view { " "sv };
    auto it = view.begin();

    EXPECT(font->glyph_or_emoji_width(it));
}

TEST_CASE(test_load_from_file)
{
    auto font = Gfx::BitmapFont::load_from_file(TEST_INPUT("TestFont.font"sv));
    EXPECT(!font->name().is_empty());
}

TEST_CASE(test_write_to_file)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

    char path[] = "/tmp/new.font.XXXXXX";
    EXPECT(mkstemp(path) != -1);
    TRY_OR_FAIL(font->write_to_file(path));
    unlink(path);
}

TEST_CASE(test_character_set_masking)
{
    auto font = TRY_OR_FAIL(Gfx::BitmapFont::try_load_from_file(TEST_INPUT("TestFont.font"sv)));

    auto unmasked_font = TRY_OR_FAIL(font->unmasked_character_set());
    EXPECT(unmasked_font->glyph_index(0x0041).value() == 0x0041);
    EXPECT(unmasked_font->glyph_index(0x0100).value() == 0x0100);
    EXPECT(unmasked_font->glyph_index(0xFFFD).value() == 0xFFFD);

    auto masked_font = TRY_OR_FAIL(unmasked_font->masked_character_set());
    EXPECT(masked_font->glyph_index(0x0041).value() == 0x0041);
    EXPECT(!masked_font->glyph_index(0x0100).has_value());
    EXPECT(masked_font->glyph_index(0xFFFD).value() == 0x1FD);
}
