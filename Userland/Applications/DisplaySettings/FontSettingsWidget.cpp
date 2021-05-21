/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FontSettingsWidget.h"
#include <Applications/DisplaySettings/FontSettingsGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/Label.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/FontDatabase.h>

namespace DisplaySettings {

FontSettingsWidget::FontSettingsWidget()
{
    load_from_gml(font_settings_gml);

    auto& default_font_label = *find_descendant_of_type_named<GUI::Label>("default_font_label");
    auto& default_font_button = *find_descendant_of_type_named<GUI::Button>("default_font_button");
    auto& fixed_width_font_label = *find_descendant_of_type_named<GUI::Label>("fixed_width_font_label");
    auto& fixed_width_font_button = *find_descendant_of_type_named<GUI::Button>("fixed_width_font_button");

    default_font_label.set_font(Gfx::FontDatabase::default_font());
    fixed_width_font_label.set_font(Gfx::FontDatabase::default_fixed_width_font());

    default_font_button.on_click = [this, &default_font_label] {
        auto font_picker = GUI::FontPicker::construct(window(), &default_font_label.font(), false);
        if (font_picker->exec() == GUI::Dialog::ExecOK) {
            default_font_label.set_font(font_picker->font());
            default_font_label.set_text(font_picker->font()->qualified_name());
        }
    };

    fixed_width_font_button.on_click = [this, &fixed_width_font_label] {
        auto font_picker = GUI::FontPicker::construct(window(), &fixed_width_font_label.font(), false);
        if (font_picker->exec() == GUI::Dialog::ExecOK) {
            fixed_width_font_label.set_font(font_picker->font());
            fixed_width_font_label.set_text(font_picker->font()->qualified_name());
        }
    };
}

FontSettingsWidget::~FontSettingsWidget()
{
}

void FontSettingsWidget::apply_settings()
{
    auto& default_font_label = *find_descendant_of_type_named<GUI::Label>("default_font_label");
    auto& fixed_width_font_label = *find_descendant_of_type_named<GUI::Label>("fixed_width_font_label");
    GUI::WindowServerConnection::the().set_system_fonts(default_font_label.text(), fixed_width_font_label.text());
}

}
