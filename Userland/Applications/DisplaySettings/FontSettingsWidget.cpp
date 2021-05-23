/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

FontSettingsWidget::FontSettingsWidget()
{
    load_from_gml(font_settings_gml);

    auto& default_font = Gfx::FontDatabase::default_font();
    m_default_font_label = *find_descendant_of_type_named<GUI::Label>("default_font_label");
    m_default_font_label->set_font(default_font);
    m_default_font_label->set_text(default_font.qualified_name());

    auto& default_font_button = *find_descendant_of_type_named<GUI::Button>("default_font_button");
    default_font_button.on_click = [this] {
        auto font_picker = GUI::FontPicker::construct(window(), &m_default_font_label->font(), false);
        if (font_picker->exec() == GUI::Dialog::ExecOK) {
            m_default_font_label->set_font(font_picker->font());
            m_default_font_label->set_text(font_picker->font()->qualified_name());
        }
    };

    auto& default_fixed_width_font = Gfx::FontDatabase::default_fixed_width_font();
    m_fixed_width_font_label = *find_descendant_of_type_named<GUI::Label>("fixed_width_font_label");
    m_fixed_width_font_label->set_font(default_fixed_width_font);
    m_fixed_width_font_label->set_text(default_fixed_width_font.qualified_name());

    auto& fixed_width_font_button = *find_descendant_of_type_named<GUI::Button>("fixed_width_font_button");
    fixed_width_font_button.on_click = [this] {
        auto font_picker = GUI::FontPicker::construct(window(), &m_fixed_width_font_label->font(), true);
        if (font_picker->exec() == GUI::Dialog::ExecOK) {
            m_fixed_width_font_label->set_font(font_picker->font());
            m_fixed_width_font_label->set_text(font_picker->font()->qualified_name());
        }
    };
}

FontSettingsWidget::~FontSettingsWidget()
{
}

void FontSettingsWidget::apply_settings()
{
    GUI::WindowServerConnection::the().set_system_fonts(m_default_font_label->text(), m_fixed_width_font_label->text());
}

}
