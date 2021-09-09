/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/BitmapFont.h>
#include <LibGfx/FontDatabase.h>
#include <LibTest/TestCase.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

TEST_CASE(test_fontdatabase_get_by_name)
{
    const char* name = "Liza 10 400";
    auto& font_database = Gfx::FontDatabase::the();
    EXPECT(!font_database.get_by_name(name)->name().is_null());
}

TEST_CASE(test_fontdatabase_get)
{
    auto& font_database = Gfx::FontDatabase::the();
    EXPECT(!font_database.get("Liza", 10, 400)->name().is_null());
}

TEST_CASE(test_fontdatabase_for_each_font)
{
    auto& font_database = Gfx::FontDatabase::the();
    font_database.for_each_font([&](const Gfx::Font& font) {
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

    const char* name = "my newly created font";
    font->set_name(name);

    EXPECT(!font->name().is_null());
    EXPECT(font->name().contains(name));
}

TEST_CASE(test_set_family)
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, 256);

    const char* family = "my newly created font family";
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

    EXPECT(font->width("A") == glyph_width);
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
    EXPECT(font->write_to_file(path));
    unlink(path);
}
