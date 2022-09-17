/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FontPluginSerenity.h"
#include <AK/String.h>
#include <LibGfx/Font/FontDatabase.h>

namespace WebContent {

FontPluginSerenity::FontPluginSerenity()
{
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

String FontPluginSerenity::generic_font_name(Web::Platform::GenericFont generic_font)
{
    // FIXME: Replace hard-coded font names with a relevant call to FontDatabase.
    // Currently, we cannot request the default font's name, or request it at a specific size and weight.
    // So, hard-coded font names it is.
    switch (generic_font) {
    case Web::Platform::GenericFont::SansSerif:
    case Web::Platform::GenericFont::UiSansSerif:
    case Web::Platform::GenericFont::Cursive:
    case Web::Platform::GenericFont::UiRounded:
        return "Katica";
    case Web::Platform::GenericFont::Monospace:
    case Web::Platform::GenericFont::UiMonospace:
        return "Csilla";
    case Web::Platform::GenericFont::Serif:
    case Web::Platform::GenericFont::UiSerif:
        return "Roman";
    case Web::Platform::GenericFont::Fantasy:
        return "Comic Book";
    case Web::Platform::GenericFont::__Count:
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

}
