/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibTest/TestCase.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

TEST_CASE(test_fontdatabase_get_by_name)
{
    auto name = "Liza 10 400 0"sv;
    auto& font_database = Gfx::FontDatabase::the();
    EXPECT(!font_database.get_by_name(name)->name().is_null());
}

TEST_CASE(test_fontdatabase_get)
{
    auto& font_database = Gfx::FontDatabase::the();
    EXPECT(!font_database.get("Liza", 10, 400, 0)->name().is_null());
}

TEST_CASE(test_fontdatabase_for_each_font)
{
    auto& font_database = Gfx::FontDatabase::the();
    font_database.for_each_font([&](Gfx::Font const& font) {
        EXPECT(!font.name().is_null());
        EXPECT(!font.qualified_name().is_null());
        EXPECT(!font.family().is_null());
        EXPECT(font.glyph_count() > 0);
    });
}

TEST_CASE(test_clone)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

    auto new_font = font->clone();
    EXPECT(!new_font->name().is_null());
    EXPECT(!new_font->qualified_name().is_null());
    EXPECT(!new_font->family().is_null());
    EXPECT(new_font->glyph_count() > 0);
}

TEST_CASE(test_set_name)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

    auto name = "my newly created font"sv;
    font->set_name(name);

    EXPECT(!font->name().is_null());
    EXPECT(font->name().contains(name));
}

TEST_CASE(test_set_family)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

    auto family = "my newly created font family"sv;
    font->set_family(family);

    EXPECT(!font->family().is_null());
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

    EXPECT(font->glyph_or_emoji_width(0));
}

TEST_CASE(test_load_from_file)
{
    auto font = Gfx::BitmapFont::load_from_file("/res/fonts/PebbletonBold14.font");
    EXPECT(!font->name().is_null());
}

TEST_CASE(test_write_to_file)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

    char path[] = "/tmp/new.font.XXXXXX";
    EXPECT(mkstemp(path) != -1);
    EXPECT(!font->write_to_file(path).is_error());
    unlink(path);
}

TEST_CASE(test_character_set_masking)
{
    auto font = Gfx::BitmapFont::try_load_from_file("/usr/Tests/LibGfx/TestFont.font");
    EXPECT(!font.is_error());

    auto unmasked_font = font.value()->unmasked_character_set();
    EXPECT(!unmasked_font.is_error());
    EXPECT(unmasked_font.value()->glyph_index(0x0041).value() == 0x0041);
    EXPECT(unmasked_font.value()->glyph_index(0x0100).value() == 0x0100);
    EXPECT(unmasked_font.value()->glyph_index(0xFFFD).value() == 0xFFFD);

    auto masked_font = unmasked_font.value()->masked_character_set();
    EXPECT(!masked_font.is_error());
    EXPECT(masked_font.value()->glyph_index(0x0041).value() == 0x0041);
    EXPECT(!masked_font.value()->glyph_index(0x0100).has_value());
    EXPECT(masked_font.value()->glyph_index(0xFFFD).value() == 0x1FD);
}
