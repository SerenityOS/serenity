/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGfx/BitmapFont.h>
#include <LibGfx/FontDatabase.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

static void test_fontdatabase_get_by_name()
{
    const char* name = "Liza 10 400";
    auto& font_database = Gfx::FontDatabase::the();
    assert(!font_database.get_by_name(name)->name().is_null());
}

static void test_fontdatabase_get()
{
    auto& font_database = Gfx::FontDatabase::the();
    assert(!font_database.get("Liza", 10, 400)->name().is_null());
}

static void test_fontdatabase_for_each_font()
{
    auto& font_database = Gfx::FontDatabase::the();
    font_database.for_each_font([&](const Gfx::Font& font) {
        assert(!font.name().is_null());
        assert(!font.qualified_name().is_null());
        assert(!font.family().is_null());
        assert(font.glyph_count() > 0);
    });
}

static void test_default_font()
{
    assert(!Gfx::FontDatabase::default_font().name().is_null());
}

static void test_default_fixed_width_font()
{
    assert(!Gfx::FontDatabase::default_fixed_width_font().name().is_null());
}

static void test_default_bold_fixed_width_font()
{
    assert(!Gfx::FontDatabase::default_bold_fixed_width_font().name().is_null());
}

static void test_default_bold_font()
{
    assert(!Gfx::FontDatabase::default_bold_font().name().is_null());
}

static void test_clone()
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, Gfx::FontTypes::Default);

    auto new_font = font->clone();
    assert(!new_font->name().is_null());
    assert(!new_font->qualified_name().is_null());
    assert(!new_font->family().is_null());
    assert(new_font->glyph_count() > 0);
}

static void test_set_name()
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, Gfx::FontTypes::Default);

    const char* name = "my newly created font";
    font->set_name(name);

    assert(!font->name().is_null());
    assert(font->name().contains(name));
}

static void test_set_family()
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, Gfx::FontTypes::Default);

    const char* family = "my newly created font family";
    font->set_family(family);

    assert(!font->family().is_null());
    assert(font->family().contains(family));
}

static void test_set_glyph_width()
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, Gfx::FontTypes::Default);

    size_t ch = 123;
    font->set_glyph_width(ch, glyph_width);

    assert(font->glyph_width(ch) == glyph_width);
}
static void test_set_glyph_spacing()
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, Gfx::FontTypes::Default);

    u8 glyph_spacing = 8;
    font->set_glyph_spacing(glyph_spacing);

    assert(font->glyph_spacing() == glyph_spacing);
}

static void test_set_type()
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, Gfx::FontTypes::Default);

    auto type = Gfx::FontTypes::Default;
    font->set_type(type);

    assert(font->type() == type);
}

static void test_width()
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, Gfx::FontTypes::Default);

    assert(font->width("A") == glyph_width);
}

static void test_glyph_or_emoji_width()
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, Gfx::FontTypes::Default);
    font->set_type(Gfx::FontTypes::Default);

    assert(font->glyph_or_emoji_width(0));
}

static void test_load_from_file()
{
    auto font = Gfx::BitmapFont::load_from_file("/res/fonts/PebbletonBold14.font");
    assert(!font->name().is_null());
}

static void test_write_to_file()
{
    u8 glyph_height = 1;
    u8 glyph_width = 1;
    auto font = Gfx::BitmapFont::create(glyph_height, glyph_width, true, Gfx::FontTypes::Default);

    char path[] = "/tmp/new.font.XXXXXX";
    assert(mkstemp(path) != -1);
    assert(font->write_to_file(path));
    unlink(path);
}

int main(int, char**)
{
#define RUNTEST(x)                      \
    {                                   \
        printf("Running " #x " ...\n"); \
        x();                            \
        printf("Success!\n");           \
    }
    RUNTEST(test_fontdatabase_get);
    RUNTEST(test_fontdatabase_get_by_name);
    RUNTEST(test_fontdatabase_for_each_font);
    RUNTEST(test_default_font);
    RUNTEST(test_default_fixed_width_font);
    RUNTEST(test_default_bold_fixed_width_font);
    RUNTEST(test_default_bold_font);
    RUNTEST(test_clone);
    RUNTEST(test_set_name);
    RUNTEST(test_set_family);
    RUNTEST(test_set_type);
    RUNTEST(test_set_glyph_width);
    RUNTEST(test_set_glyph_spacing);
    RUNTEST(test_width);
    RUNTEST(test_glyph_or_emoji_width);
    RUNTEST(test_load_from_file);
    RUNTEST(test_write_to_file);
    printf("PASS\n");

    return 0;
}
