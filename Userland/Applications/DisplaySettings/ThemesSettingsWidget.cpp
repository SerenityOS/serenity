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
#include <LibGUI/CheckBox.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Process.h>

namespace DisplaySettings {

static DeprecatedString get_color_scheme_name_from_pathname(DeprecatedString const& color_scheme_path)
{
    return color_scheme_path.replace("/res/color-schemes/"sv, ""sv, ReplaceMode::FirstOnly).replace(".ini"sv, ""sv, ReplaceMode::FirstOnly);
};

ThemesSettingsWidget::ThemesSettingsWidget(bool& background_settings_changed)
    : m_background_settings_changed { background_settings_changed }
{
    load_from_gml(themes_settings_gml).release_value_but_fixme_should_propagate_errors();
    m_themes = MUST(Gfx::list_installed_system_themes());

    size_t current_theme_index;
    auto current_theme_name = GUI::ConnectionToWindowServer::the().get_system_theme();
    m_theme_names.ensure_capacity(m_themes.size());
    for (auto& theme_meta : m_themes) {
        m_theme_names.append(theme_meta.name);
        if (current_theme_name == theme_meta.name) {
            m_selected_theme = &theme_meta;
            current_theme_index = m_theme_names.size() - 1;
        }
    }
    m_theme_preview = find_descendant_of_type_named<GUI::Frame>("preview_frame")->add<ThemePreviewWidget>(palette());
    m_themes_combo = *find_descendant_of_type_named<GUI::ComboBox>("themes_combo");
    m_themes_combo->set_only_allow_values_from_model(true);
    m_themes_combo->set_model(*GUI::ItemListModel<DeprecatedString>::create(m_theme_names));
    m_themes_combo->on_change = [this](auto&, const GUI::ModelIndex& index) {
        m_selected_theme = &m_themes.at(index.row());
        auto set_theme_result = m_theme_preview->set_theme(m_selected_theme->path);
        if (set_theme_result.is_error()) {
            GUI::MessageBox::show_error(window(), DeprecatedString::formatted("There was an error generating the theme preview: {}", set_theme_result.error()));
        }
        set_modified(true);
    };
    m_themes_combo->set_selected_index(current_theme_index, GUI::AllowCallback::No);

    auto mouse_settings_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/app-mouse.png"sv).release_value_but_fixme_should_propagate_errors();

    m_color_scheme_names.clear();
    Core::DirIterator iterator("/res/color-schemes", Core::DirIterator::SkipParentAndBaseDir);
    while (iterator.has_next()) {
        auto path = iterator.next_path();
        m_color_scheme_names.append(path.replace(".ini"sv, ""sv, ReplaceMode::FirstOnly));
    }
    quick_sort(m_color_scheme_names);
    auto& color_scheme_combo = *find_descendant_of_type_named<GUI::ComboBox>("color_scheme_combo");
    color_scheme_combo.set_only_allow_values_from_model(true);
    color_scheme_combo.set_model(*GUI::ItemListModel<DeprecatedString>::create(m_color_scheme_names));
    m_selected_color_scheme_name = get_color_scheme_name_from_pathname(GUI::Widget::palette().color_scheme_path());
    auto selected_color_scheme_index = m_color_scheme_names.find_first_index(m_selected_color_scheme_name);
    if (selected_color_scheme_index.has_value())
        color_scheme_combo.set_selected_index(selected_color_scheme_index.value());
    else {
        color_scheme_combo.set_text("Custom");
        m_color_scheme_is_file_based = false;
        if (m_color_scheme_names.size() > 1) {
            color_scheme_combo.set_enabled(true);
            find_descendant_of_type_named<GUI::CheckBox>("custom_color_scheme_checkbox")->set_checked(true);
        }
    }
    color_scheme_combo.on_change = [this](auto&, const GUI::ModelIndex& index) {
        m_selected_color_scheme_name = index.data().as_string();
        m_color_scheme_is_file_based = true;
        set_modified(true);
    };

    find_descendant_of_type_named<GUI::CheckBox>("custom_color_scheme_checkbox")->on_checked = [this](bool) {
        if (m_color_scheme_names.size() <= 1)
            return;

        if (find_descendant_of_type_named<GUI::CheckBox>("custom_color_scheme_checkbox")->is_checked())
            find_descendant_of_type_named<GUI::ComboBox>("color_scheme_combo")->set_enabled(true);
        else {
            find_descendant_of_type_named<GUI::ComboBox>("color_scheme_combo")->set_enabled(false);
            set_modified(true);
        }
    };

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
                auto set_theme_result = m_theme_preview->set_theme(m_selected_theme->path);
                if (set_theme_result.is_error()) {
                    GUI::MessageBox::show_error(window(), DeprecatedString::formatted("There was an error setting the new theme: {}", set_theme_result.error()));
                }
            }
            ++index;
        }
    };
}

void ThemesSettingsWidget::apply_settings()
{
    Optional<DeprecatedString> color_scheme_path = DeprecatedString::formatted("/res/color-schemes/{}.ini", m_selected_color_scheme_name);

    if (!m_color_scheme_is_file_based && find_descendant_of_type_named<GUI::CheckBox>("custom_color_scheme_checkbox")->is_checked())
        VERIFY(GUI::ConnectionToWindowServer::the().set_system_theme(m_selected_theme->path, m_selected_theme->name, m_background_settings_changed, "Custom"sv));
    else if (find_descendant_of_type_named<GUI::CheckBox>("custom_color_scheme_checkbox")->is_checked())
        VERIFY(GUI::ConnectionToWindowServer::the().set_system_theme(m_selected_theme->path, m_selected_theme->name, m_background_settings_changed, color_scheme_path));
    else {
        VERIFY(GUI::ConnectionToWindowServer::the().set_system_theme(m_selected_theme->path, m_selected_theme->name, m_background_settings_changed, OptionalNone()));
        // Update the color scheme combobox to match the new theme.
        auto const theme_config = Core::ConfigFile::open(m_selected_theme->path).release_value_but_fixme_should_propagate_errors();
        auto const color_scheme_index = m_color_scheme_names.find_first_index(get_color_scheme_name_from_pathname(theme_config->read_entry("Paths", "ColorScheme")));
        if (color_scheme_index.has_value())
            find_descendant_of_type_named<GUI::ComboBox>("color_scheme_combo")->set_selected_index(color_scheme_index.value());
    }

    m_background_settings_changed = false;
}

}
