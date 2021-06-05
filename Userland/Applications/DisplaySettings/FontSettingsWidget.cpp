/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Thomas Keppler <winfr34k@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FontSettingsWidget.h"
#include <Applications/DisplaySettings/FontSettingsGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/FontDatabase.h>

namespace DisplaySettings {

static void update_label_with_font(GUI::Label&, Gfx::Font const&);

FontSettingsWidget::FontSettingsWidget()
{
    load_from_gml(font_settings_gml);

    auto& default_font = Gfx::FontDatabase::default_font();
    m_default_font_label = *find_descendant_of_type_named<GUI::Label>("default_font_label");
    update_label_with_font(*m_default_font_label, default_font);

    auto& default_font_button = *find_descendant_of_type_named<GUI::Button>("default_font_button");
    default_font_button.on_click = [this](auto) {
        auto font_picker = GUI::FontPicker::construct(window(), &m_default_font_label->font(), false);
        if (font_picker->exec() == GUI::Dialog::ExecOK) {
            update_label_with_font(*m_default_font_label, *font_picker->font());
        }
    };

    auto& default_fixed_width_font = Gfx::FontDatabase::default_fixed_width_font();
    m_fixed_width_font_label = *find_descendant_of_type_named<GUI::Label>("fixed_width_font_label");
    update_label_with_font(*m_fixed_width_font_label, default_fixed_width_font);

    auto& fixed_width_font_button = *find_descendant_of_type_named<GUI::Button>("fixed_width_font_button");
    fixed_width_font_button.on_click = [this](auto) {
        auto font_picker = GUI::FontPicker::construct(window(), &m_fixed_width_font_label->font(), true);
        if (font_picker->exec() == GUI::Dialog::ExecOK) {
            update_label_with_font(*m_fixed_width_font_label, *font_picker->font());
        }
    };
}

FontSettingsWidget::~FontSettingsWidget()
{
}

static void update_label_with_font(GUI::Label& label, Gfx::Font const& font)
{
    label.set_text(font.qualified_name());
    label.set_font(font);
}

void FontSettingsWidget::apply_settings()
{
    GUI::WindowServerConnection::the().set_system_fonts(m_default_font_label->text(), m_fixed_width_font_label->text());
}

}
