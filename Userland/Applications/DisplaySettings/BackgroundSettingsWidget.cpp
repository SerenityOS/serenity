/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BackgroundSettingsWidget.h"
#include <AK/StringBuilder.h>
#include <Applications/DisplaySettings/BackgroundSettingsGML.h>
#include <LibCore/ConfigFile.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/IconView.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>

// Including this after to avoid LibIPC errors
#include <LibConfig/Client.h>

namespace DisplaySettings {

BackgroundSettingsWidget::BackgroundSettingsWidget()
{
    m_modes.append("tile");
    m_modes.append("center");
    m_modes.append("stretch");

    create_frame();
    load_current_settings();
}

void BackgroundSettingsWidget::create_frame()
{
    load_from_gml(background_settings_gml);

    m_monitor_widget = *find_descendant_of_type_named<DisplaySettings::MonitorWidget>("monitor_widget");

    m_wallpaper_view = *find_descendant_of_type_named<GUI::IconView>("wallpaper_view");
    m_wallpaper_view->set_model(GUI::FileSystemModel::create("/res/wallpapers"));
    m_wallpaper_view->set_model_column(GUI::FileSystemModel::Column::Name);
    m_wallpaper_view->on_selection_change = [this] {
        String path;
        if (m_wallpaper_view->selection().is_empty()) {
            path = "";
        } else {
            auto index = m_wallpaper_view->selection().first();
            path = static_cast<GUI::FileSystemModel*>(m_wallpaper_view->model())->full_path(index);
        }

        m_monitor_widget->set_wallpaper(path);
    };

    m_context_menu = GUI::Menu::construct();
    m_show_in_file_manager_action = GUI::Action::create("Show in File Manager", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-file-manager.png").release_value_but_fixme_should_propagate_errors(), [this](GUI::Action const&) {
        LexicalPath path { m_monitor_widget->wallpaper() };
        Desktop::Launcher::open(URL::create_with_file_protocol(path.dirname(), path.basename()));
    });
    m_context_menu->add_action(*m_show_in_file_manager_action);

    m_context_menu->add_separator();
    m_copy_action = GUI::CommonActions::make_copy_action([this](auto&) { GUI::Clipboard::the().set_plain_text(m_monitor_widget->wallpaper()); }, this);
    m_context_menu->add_action(*m_copy_action);

    m_wallpaper_view->on_context_menu_request = [&](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (index.is_valid()) {
            m_context_menu->popup(event.screen_position(), m_show_in_file_manager_action);
        }
    };

    auto& button = *find_descendant_of_type_named<GUI::Button>("wallpaper_open_button");
    button.on_click = [this](auto) {
        auto path = GUI::FilePicker::get_open_filepath(window(), "Select wallpaper from file system", "/res/wallpapers");
        if (!path.has_value())
            return;
        m_wallpaper_view->selection().clear();
        m_monitor_widget->set_wallpaper(path.value());
    };

    m_mode_combo = *find_descendant_of_type_named<GUI::ComboBox>("mode_combo");
    m_mode_combo->set_only_allow_values_from_model(true);
    m_mode_combo->set_model(*GUI::ItemListModel<String>::create(m_modes));
    m_mode_combo->on_change = [this](auto&, const GUI::ModelIndex& index) {
        m_monitor_widget->set_wallpaper_mode(m_modes.at(index.row()));
    };

    m_color_input = *find_descendant_of_type_named<GUI::ColorInput>("color_input");
    m_color_input->set_color_has_alpha_channel(false);
    m_color_input->set_color_picker_title("Select color for desktop");
    m_color_input->on_change = [this] {
        m_monitor_widget->set_background_color(m_color_input->color());
    };
}

void BackgroundSettingsWidget::load_current_settings()
{
    auto ws_config = Core::ConfigFile::open("/etc/WindowServer.ini").release_value_but_fixme_should_propagate_errors();

    auto selected_wallpaper = Config::read_string("WindowManager", "Background", "Wallpaper", "");
    if (!selected_wallpaper.is_empty()) {
        auto index = static_cast<GUI::FileSystemModel*>(m_wallpaper_view->model())->index(selected_wallpaper, m_wallpaper_view->model_column());
        m_wallpaper_view->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
        m_monitor_widget->set_wallpaper(selected_wallpaper);
    }

    auto mode = ws_config->read_entry("Background", "Mode", "center");
    if (!m_modes.contains_slow(mode)) {
        warnln("Invalid background mode '{}' in WindowServer config, falling back to 'center'", mode);
        mode = "center";
    }
    m_monitor_widget->set_wallpaper_mode(mode);
    m_mode_combo->set_selected_index(m_modes.find_first_index(mode).value_or(0));

    auto palette_desktop_color = palette().desktop_background();
    auto background_color = ws_config->read_entry("Background", "Color", "");

    if (!background_color.is_empty()) {
        auto opt_color = Color::from_string(background_color);
        if (opt_color.has_value())
            palette_desktop_color = opt_color.value();
    }

    m_color_input->set_color(palette_desktop_color);
    m_monitor_widget->set_background_color(palette_desktop_color);
}

void BackgroundSettingsWidget::apply_settings()
{
    if (!GUI::Desktop::the().set_wallpaper(m_monitor_widget->wallpaper_bitmap(), m_monitor_widget->wallpaper()))
        GUI::MessageBox::show_error(window(), String::formatted("Unable to load file {} as wallpaper", m_monitor_widget->wallpaper()));

    GUI::Desktop::the().set_background_color(m_color_input->text());
    GUI::Desktop::the().set_wallpaper_mode(m_monitor_widget->wallpaper_mode());
}

}
