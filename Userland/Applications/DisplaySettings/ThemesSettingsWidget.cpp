/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ThemesSettingsWidget.h"
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <Applications/DisplaySettings/ThemesSettingsGML.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Application.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Process.h>

namespace DisplaySettings {

ThemesSettingsWidget::ThemesSettingsWidget(bool& background_settings_changed)
    : m_background_settings_changed { background_settings_changed }
{
    load_from_gml(themes_settings_gml);
    m_themes = Gfx::list_installed_system_themes();

    size_t current_theme_index;
    auto current_theme_name = GUI::ConnectionToWindowServer::the().get_system_theme();
    auto theme_overridden = GUI::ConnectionToWindowServer::the().is_system_theme_overridden();
    m_theme_names.ensure_capacity(m_themes.size());
    for (auto& theme_meta : m_themes) {
        m_theme_names.append(theme_meta.name);
        if (!theme_overridden && current_theme_name == theme_meta.name) {
            m_selected_theme = &theme_meta;
            current_theme_index = m_theme_names.size() - 1;
        }
    }
    m_theme_preview = find_descendant_of_type_named<GUI::Frame>("preview_frame")->add<ThemePreviewWidget>(palette());
    m_themes_combo = *find_descendant_of_type_named<GUI::ComboBox>("themes_combo");
    m_themes_combo->set_only_allow_values_from_model(true);
    m_themes_combo->set_model(*GUI::ItemListModel<String>::create(m_theme_names));
    m_themes_combo->on_change = [this](auto&, const GUI::ModelIndex& index) {
        m_selected_theme = &m_themes.at(index.row());
        m_theme_preview->set_theme(m_selected_theme->path);
        set_modified(true);
    };
    m_themes_combo->set_selected_index(current_theme_index, GUI::AllowCallback::No);

    auto mouse_settings_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-mouse.png"sv).release_value_but_fixme_should_propagate_errors();
    m_cursor_themes_button = *find_descendant_of_type_named<GUI::Button>("cursor_themes_button");
    m_cursor_themes_button->set_icon(mouse_settings_icon);
    m_cursor_themes_button->on_click = [&](auto) {
        GUI::Process::spawn_or_show_error(window(), "/bin/MouseSettings"sv, Array { "-t", "cursor-theme" });
    };

    GUI::Application::the()->on_theme_change = [&]() {
        auto theme_override = GUI::ConnectionToWindowServer::the().get_system_theme_override();
        if (theme_override.has_value()) {
            m_themes_combo->clear_selection();
            static_cast<RefPtr<GUI::AbstractThemePreview>>(m_theme_preview)->set_theme(*theme_override);
            return;
        }

        auto current_theme_name = GUI::ConnectionToWindowServer::the().get_system_theme();

        size_t index = 0;
        for (auto& theme_meta : m_themes) {
            if (current_theme_name == theme_meta.name) {
                m_themes_combo->set_selected_index(index, GUI::AllowCallback::No);
                m_selected_theme = &m_themes.at(index);
                m_theme_preview->set_theme(m_selected_theme->path);
            }
            ++index;
        }
    };
}

void ThemesSettingsWidget::apply_settings()
{
    if (m_selected_theme && m_selected_theme->name != GUI::ConnectionToWindowServer::the().get_system_theme())
        VERIFY(GUI::ConnectionToWindowServer::the().set_system_theme(m_selected_theme->path, m_selected_theme->name, m_background_settings_changed));
    m_background_settings_changed = false;
}

}
