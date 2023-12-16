/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ThemesSettingsWidget.h"
#include <AK/QuickSort.h>
#include <Applications/DisplaySettings/ThemesSettingsGML.h>
#include <LibCore/Directory.h>
#include <LibGUI/Application.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Process.h>

namespace DisplaySettings {

static ErrorOr<String> get_color_scheme_name_from_pathname(StringView color_scheme_path)
{
    return TRY(String::from_byte_string(color_scheme_path.replace("/res/color-schemes/"sv, ""sv, ReplaceMode::FirstOnly).replace(".ini"sv, ""sv, ReplaceMode::FirstOnly)));
}

ErrorOr<NonnullRefPtr<ThemesSettingsWidget>> ThemesSettingsWidget::try_create(bool& background_settings_changed)
{
    auto theme_settings_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ThemesSettingsWidget(background_settings_changed)));
    TRY(theme_settings_widget->load_from_gml(themes_settings_gml));
    TRY(theme_settings_widget->setup_interface());

    return theme_settings_widget;
}

ErrorOr<void> ThemesSettingsWidget::setup_interface()
{
    m_themes = TRY(Gfx::list_installed_system_themes());

    size_t current_theme_index;
    auto current_theme_name = GUI::ConnectionToWindowServer::the().get_system_theme();
    TRY(m_theme_names.try_ensure_capacity(m_themes.size()));
    for (auto& theme_meta : m_themes) {
        TRY(m_theme_names.try_append(TRY(String::from_byte_string(theme_meta.name))));
        if (current_theme_name == theme_meta.name) {
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
        auto selected_theme_path = String::from_byte_string(m_selected_theme->path);
        ErrorOr<void> set_theme_result;
        if (!selected_theme_path.is_error())
            set_theme_result = m_theme_preview->set_theme(selected_theme_path.release_value());
        if (set_theme_result.is_error()) {
            auto detailed_error_message = String::formatted("There was an error generating the theme preview: {}", set_theme_result.error());
            if (!detailed_error_message.is_error())
                GUI::MessageBox::show_error(window(), detailed_error_message.release_value());
            else
                GUI::MessageBox::show_error(window(), "There was an error generating the theme preview"sv);
            return;
        }
        set_modified(true);
    };
    m_themes_combo->set_selected_index(current_theme_index, GUI::AllowCallback::No);

    auto mouse_settings_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-mouse.png"sv));

    m_color_scheme_names.clear();
    TRY(Core::Directory::for_each_entry("/res/color-schemes"sv, Core::DirIterator::SkipParentAndBaseDir, [&](auto& entry, auto&) -> ErrorOr<IterationDecision> {
        LexicalPath path { entry.name };
        TRY(m_color_scheme_names.try_append(TRY(String::from_utf8(path.title()))));
        return IterationDecision::Continue;
    }));
    quick_sort(m_color_scheme_names);
    auto& color_scheme_combo = *find_descendant_of_type_named<GUI::ComboBox>("color_scheme_combo");
    color_scheme_combo.set_only_allow_values_from_model(true);
    color_scheme_combo.set_model(*GUI::ItemListModel<String>::create(m_color_scheme_names));
    m_selected_color_scheme_name = TRY(get_color_scheme_name_from_pathname(GUI::Widget::palette().color_scheme_path()));
    auto selected_color_scheme_index = m_color_scheme_names.find_first_index(m_selected_color_scheme_name);
    if (selected_color_scheme_index.has_value())
        color_scheme_combo.set_selected_index(selected_color_scheme_index.value());
    else {
        color_scheme_combo.set_text("Custom");
        m_color_scheme_is_file_based = false;
    }

    auto theme_config = TRY(Core::ConfigFile::open(m_selected_theme->path));
    if (!selected_color_scheme_index.has_value() || GUI::Widget::palette().color_scheme_path() != theme_config->read_entry("Paths", "ColorScheme")) {
        if (m_color_scheme_names.size() > 1) {
            color_scheme_combo.set_enabled(true);
            find_descendant_of_type_named<GUI::CheckBox>("custom_color_scheme_checkbox")->set_checked(true);
        }
    }
    color_scheme_combo.on_change = [this](auto&, const GUI::ModelIndex& index) {
        auto result = String::from_byte_string(index.data().as_string());
        if (result.is_error())
            return;
        m_selected_color_scheme_name = result.release_value();
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
                auto selected_theme_path = String::from_byte_string(m_selected_theme->path);
                ErrorOr<void> set_theme_result;
                if (!selected_theme_path.is_error())
                    set_theme_result = m_theme_preview->set_theme(selected_theme_path.release_value());
                if (set_theme_result.is_error()) {
                    auto detailed_error_message = String::formatted("There was an error generating the theme preview: {}", set_theme_result.error());
                    if (!detailed_error_message.is_error())
                        GUI::MessageBox::show_error(window(), detailed_error_message.release_value());
                    else
                        GUI::MessageBox::show_error(window(), "There was an error generating the theme preview"sv);
                }
            }
            ++index;
        }
    };

    return {};
}

ThemesSettingsWidget::ThemesSettingsWidget(bool& background_settings_changed)
    : m_background_settings_changed { background_settings_changed }
{
}

void ThemesSettingsWidget::apply_settings()
{
    m_background_settings_changed = false;
    auto color_scheme_path_or_error = String::formatted("/res/color-schemes/{}.ini", m_selected_color_scheme_name);
    if (color_scheme_path_or_error.is_error()) {
        GUI::MessageBox::show_error(window(), "Unable to apply changes"sv);
        return;
    }
    auto color_scheme_path = color_scheme_path_or_error.release_value();

    if (!m_color_scheme_is_file_based && find_descendant_of_type_named<GUI::CheckBox>("custom_color_scheme_checkbox")->is_checked()) {
        auto set_theme_result = GUI::ConnectionToWindowServer::the().set_system_theme(m_selected_theme->path, m_selected_theme->name, m_background_settings_changed, "Custom"sv);
        if (!set_theme_result)
            GUI::MessageBox::show_error(window(), "Failed to apply theme settings"sv);
    } else if (find_descendant_of_type_named<GUI::CheckBox>("custom_color_scheme_checkbox")->is_checked()) {
        auto set_theme_result = GUI::ConnectionToWindowServer::the().set_system_theme(m_selected_theme->path, m_selected_theme->name, m_background_settings_changed, color_scheme_path.to_byte_string());
        if (!set_theme_result)
            GUI::MessageBox::show_error(window(), "Failed to apply theme settings"sv);
    } else {
        auto theme_config = Core::ConfigFile::open(m_selected_theme->path);
        if (theme_config.is_error()) {
            GUI::MessageBox::show_error(window(), "Failed to open theme config file"sv);
            return;
        }
        auto preferred_color_scheme_path = get_color_scheme_name_from_pathname(theme_config.release_value()->read_entry("Paths", "ColorScheme"));
        auto set_theme_result = GUI::ConnectionToWindowServer::the().set_system_theme(m_selected_theme->path, m_selected_theme->name, m_background_settings_changed, OptionalNone());
        if (!set_theme_result || preferred_color_scheme_path.is_error()) {
            GUI::MessageBox::show_error(window(), "Failed to apply theme settings"sv);
            return;
        }
        // Update the color scheme combobox to match the new theme.
        auto const color_scheme_index = m_color_scheme_names.find_first_index(preferred_color_scheme_path.value());
        if (color_scheme_index.has_value())
            find_descendant_of_type_named<GUI::ComboBox>("color_scheme_combo")->set_selected_index(color_scheme_index.value());
    }
}

}
