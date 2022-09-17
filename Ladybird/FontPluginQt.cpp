/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "FontPluginQt.h"
#include <AK/String.h>
#include <LibGfx/Font/FontDatabase.h>
#include <QFont>
#include <QFontInfo>

extern String s_serenity_resource_root;

namespace Ladybird {

FontPluginQt::FontPluginQt()
{
    // Load the default SerenityOS fonts...
    Gfx::FontDatabase::set_default_fonts_lookup_path(String::formatted("{}/res/fonts", s_serenity_resource_root));

    // ...and also anything we can find in /usr/share/fonts
    Gfx::FontDatabase::the().load_all_fonts_from_path("/usr/share/fonts");

    Gfx::FontDatabase::set_default_font_query("Katica 10 400 0");
    Gfx::FontDatabase::set_fixed_width_font_query("Csilla 10 400 0");

    update_generic_fonts();

    auto default_font_name = generic_font_name(Web::Platform::GenericFont::UiSansSerif);
    m_default_font = Gfx::FontDatabase::the().get(default_font_name, 12.0, 400, 0);
    VERIFY(m_default_font);
}

FontPluginQt::~FontPluginQt() = default;

Gfx::Font& FontPluginQt::default_font()
{
    return *m_default_font;
}

Gfx::Font& FontPluginQt::default_fixed_width_font()
{
    return *m_default_fixed_width_font;
}

void FontPluginQt::update_generic_fonts()
{
    // How we choose which system font to use for each CSS font:
    // 1. Ask Qt via the QFont::StyleHint mechanism for the user's preferred font.
    // 2. Try loading that font through Gfx::FontDatabase
    // 3. If we don't support that font for whatever reason (e.g missing TrueType features in LibGfx)...
    //    1. Try a list of known-suitable fallback fonts with their names hard-coded below
    //    2. If that didn't work, fall back to Gfx::FontDatabase::default_font() (or default_fixed_width_font())

    // This is rather weird, but it's how things work right now, as we can only draw with fonts loaded by LibGfx.

    m_generic_font_names.resize(static_cast<size_t>(Web::Platform::GenericFont::__Count));

    auto update_mapping = [&](Web::Platform::GenericFont generic_font, QFont::StyleHint qfont_style_hint, Vector<String> fallbacks = {}) {
        QFont qt_font;
        qt_font.setStyleHint(qfont_style_hint);
        QFontInfo qt_info(qt_font);
        auto qt_font_family = qt_info.family();

        auto gfx_font = Gfx::FontDatabase::the().get(qt_font_family.toUtf8().data(), 16, 400, 0, Gfx::Font::AllowInexactSizeMatch::Yes);
        if (!gfx_font) {
            for (auto& fallback : fallbacks) {
                gfx_font = Gfx::FontDatabase::the().get(fallback, 16, 400, 0, Gfx::Font::AllowInexactSizeMatch::Yes);
                if (gfx_font)
                    break;
            }
        }

        if (!gfx_font) {
            if (generic_font == Web::Platform::GenericFont::Monospace || generic_font == Web::Platform::GenericFont::UiMonospace)
                gfx_font = Gfx::FontDatabase::default_fixed_width_font();
            else
                gfx_font = Gfx::FontDatabase::default_font();
        }

        m_generic_font_names[static_cast<size_t>(generic_font)] = gfx_font->family();
    };

    // Fallback fonts to look for if Gfx::Font can't load the font suggested by Qt.
    // The lists are basically arbitrary, taken from https://www.w3.org/Style/Examples/007/fonts.en.html
    Vector<String> cursive_fallbacks { "Comic Sans MS", "Comic Sans", "Apple Chancery", "Bradley Hand", "Brush Script MT", "Snell Roundhand", "URW Chancery L" };
    Vector<String> fantasy_fallbacks { "Impact", "Luminari", "Chalkduster", "Jazz LET", "Blippo", "Stencil Std", "Marker Felt", "Trattatello" };
    Vector<String> monospace_fallbacks { "Andale Mono", "Courier New", "Courier", "FreeMono", "OCR A Std", "DejaVu Sans Mono", "Liberation Mono", "Csilla" };
    Vector<String> sans_serif_fallbacks { "Arial", "Helvetica", "Verdana", "Trebuchet MS", "Gill Sans", "Noto Sans", "Avantgarde", "Optima", "Arial Narrow", "Liberation Sans", "Katica" };
    Vector<String> serif_fallbacks { "Times", "Times New Roman", "Didot", "Georgia", "Palatino", "Bookman", "New Century Schoolbook", "American Typewriter", "Liberation Serif", "Roman" };

    update_mapping(Web::Platform::GenericFont::Cursive, QFont::StyleHint::Cursive, cursive_fallbacks);
    update_mapping(Web::Platform::GenericFont::Fantasy, QFont::StyleHint::Fantasy, fantasy_fallbacks);
    update_mapping(Web::Platform::GenericFont::Monospace, QFont::StyleHint::Monospace, monospace_fallbacks);
    update_mapping(Web::Platform::GenericFont::SansSerif, QFont::StyleHint::SansSerif, sans_serif_fallbacks);
    update_mapping(Web::Platform::GenericFont::Serif, QFont::StyleHint::Serif, serif_fallbacks);
    update_mapping(Web::Platform::GenericFont::UiMonospace, QFont::StyleHint::Monospace, monospace_fallbacks);
    update_mapping(Web::Platform::GenericFont::UiRounded, QFont::StyleHint::SansSerif, sans_serif_fallbacks);
    update_mapping(Web::Platform::GenericFont::UiSansSerif, QFont::StyleHint::SansSerif, sans_serif_fallbacks);
    update_mapping(Web::Platform::GenericFont::UiSerif, QFont::StyleHint::Serif, serif_fallbacks);
}

String FontPluginQt::generic_font_name(Web::Platform::GenericFont generic_font)
{
    return m_generic_font_names[static_cast<size_t>(generic_font)];
}

}
