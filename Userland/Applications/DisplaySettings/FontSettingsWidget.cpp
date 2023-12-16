/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Thomas Keppler <winfr34k@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FontSettingsWidget.h"
#include <Applications/DisplaySettings/FontSettingsGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/FontPicker.h>
#include <LibGfx/Font/FontDatabase.h>

namespace DisplaySettings {

static void update_label_with_font(GUI::Label&, Gfx::Font const&);

ErrorOr<NonnullRefPtr<FontSettingsWidget>> FontSettingsWidget::try_create()
{
    auto font_settings_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) FontSettingsWidget()));
    TRY(font_settings_widget->setup_interface());
    return font_settings_widget;
}

ErrorOr<void> FontSettingsWidget::setup_interface()
{
    TRY(load_from_gml(font_settings_gml));

    auto& default_font = Gfx::FontDatabase::default_font();
    m_default_font_label = *find_descendant_of_type_named<GUI::Label>("default_font_label");
    update_label_with_font(*m_default_font_label, default_font);

    auto& default_font_button = *find_descendant_of_type_named<GUI::Button>("default_font_button");
    default_font_button.on_click = [this](auto) {
        auto font_picker = GUI::FontPicker::construct(window(), &m_default_font_label->font(), false);
        if (font_picker->exec() == GUI::Dialog::ExecResult::OK) {
            update_label_with_font(*m_default_font_label, *font_picker->font());
            set_modified(true);
        }
    };

    auto& window_title_font = Gfx::FontDatabase::window_title_font();
    m_window_title_font_label = *find_descendant_of_type_named<GUI::Label>("window_title_font_label");
    update_label_with_font(*m_window_title_font_label, window_title_font);

    auto& window_title_font_button = *find_descendant_of_type_named<GUI::Button>("window_title_font_button");
    window_title_font_button.on_click = [this](auto) {
        auto font_picker = GUI::FontPicker::construct(window(), &m_window_title_font_label->font(), false);
        if (font_picker->exec() == GUI::Dialog::ExecResult::OK) {
            update_label_with_font(*m_window_title_font_label, *font_picker->font());
            set_modified(true);
        }
    };

    auto& default_fixed_width_font = Gfx::FontDatabase::default_fixed_width_font();
    m_fixed_width_font_label = *find_descendant_of_type_named<GUI::Label>("fixed_width_font_label");
    update_label_with_font(*m_fixed_width_font_label, default_fixed_width_font);

    auto& fixed_width_font_button = *find_descendant_of_type_named<GUI::Button>("fixed_width_font_button");
    fixed_width_font_button.on_click = [this](auto) {
        auto font_picker = GUI::FontPicker::construct(window(), &m_fixed_width_font_label->font(), true);
        if (font_picker->exec() == GUI::Dialog::ExecResult::OK) {
            update_label_with_font(*m_fixed_width_font_label, *font_picker->font());
            set_modified(true);
        }
    };

    return {};
}

static void update_label_with_font(GUI::Label& label, Gfx::Font const& font)
{
    label.set_text(font.human_readable_name());
    label.set_font(font);
}

void FontSettingsWidget::apply_settings()
{
    GUI::ConnectionToWindowServer::the().set_system_fonts(
        m_default_font_label->font().qualified_name().to_byte_string(),
        m_fixed_width_font_label->font().qualified_name().to_byte_string(),
        m_window_title_font_label->font().qualified_name().to_byte_string());
}

}
