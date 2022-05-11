/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ThemesSettingsWidget.h"
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <Applications/DisplaySettings/ThemesSettingsGML.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/ItemListModel.h>

namespace DisplaySettings {

static inline String current_system_theme()
{
    return GUI::ConnectionToWindowServer::the().get_system_theme();
}

ThemesSettingsWidget::ThemesSettingsWidget(bool& background_settings_changed)
    : m_background_settings_changed { background_settings_changed }
{
    load_from_gml(themes_settings_gml);
    m_themes = Gfx::list_installed_system_themes();

    size_t current_theme_index;
    auto current_theme_name = current_system_theme();
    m_theme_names.ensure_capacity(m_themes.size());
    for (auto& theme_meta : m_themes) {
        m_theme_names.append(theme_meta.name);
        if (current_theme_name == theme_meta.name) {
            m_selected_theme = &theme_meta;
            current_theme_index = m_theme_names.size() - 1;
        }
    }
    VERIFY(m_selected_theme);

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
}

void ThemesSettingsWidget::apply_settings()
{
    if (m_selected_theme && m_selected_theme->name != current_system_theme())
        VERIFY(GUI::ConnectionToWindowServer::the().set_system_theme(m_selected_theme->path, m_selected_theme->name, m_background_settings_changed));
    m_background_settings_changed = false;
}

}
