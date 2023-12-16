/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FontPluginSerenity.h"
#include <AK/ByteString.h>
#include <LibGfx/Font/FontDatabase.h>

namespace Web::Platform {

FontPluginSerenity::FontPluginSerenity()
{
    // NOTE: These will eventually get replaced by system defaults.
    Gfx::FontDatabase::set_default_font_query("Katica 10 400 0");
    Gfx::FontDatabase::set_fixed_width_font_query("Csilla 10 400 0");
}

FontPluginSerenity::~FontPluginSerenity() = default;

Gfx::Font& FontPluginSerenity::default_font()
{
    return Gfx::FontDatabase::default_font();
}

Gfx::Font& FontPluginSerenity::default_fixed_width_font()
{
    return Gfx::FontDatabase::default_fixed_width_font();
}

FlyString FontPluginSerenity::generic_font_name(GenericFont generic_font)
{
    // FIXME: Make these configurable at the browser settings level. Fall back to system defaults.
    switch (generic_font) {
    case GenericFont::SansSerif:
    case GenericFont::UiSansSerif:
    case GenericFont::Cursive:
    case GenericFont::UiRounded:
        return default_font().family();
    case GenericFont::Monospace:
    case GenericFont::UiMonospace:
        return default_fixed_width_font().family();
    case GenericFont::Serif:
    case GenericFont::UiSerif:
        return "Roman"_fly_string;
    case GenericFont::Fantasy:
        return "Comic Book"_fly_string;
    case GenericFont::__Count:
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

}
