/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "DisplaySettings.h"
#include <AK/StringBuilder.h>
#include <Applications/DisplaySettings/DisplaySettingsWindowGML.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>

REGISTER_WIDGET(DisplaySettings, MonitorWidget)

DisplaySettingsWidget::DisplaySettingsWidget()
{
    create_resolution_list();
    create_wallpaper_list();

    create_frame();

    load_current_settings();
}

void DisplaySettingsWidget::create_resolution_list()
{
    // TODO: Find a better way to get the default resolution
    m_resolutions.append({ 640, 480 });
    m_resolutions.append({ 800, 600 });
    m_resolutions.append({ 1024, 768 });
    m_resolutions.append({ 1280, 720 });
    m_resolutions.append({ 1280, 768 });
    m_resolutions.append({ 1280, 960 });
    m_resolutions.append({ 1280, 1024 });
    m_resolutions.append({ 1360, 768 });
    m_resolutions.append({ 1368, 768 });
    m_resolutions.append({ 1440, 900 });
    m_resolutions.append({ 1600, 900 });
    m_resolutions.append({ 1600, 1200 });
    m_resolutions.append({ 1920, 1080 });
    m_resolutions.append({ 2048, 1152 });
    m_resolutions.append({ 2560, 1080 });
    m_resolutions.append({ 2560, 1440 });
}

void DisplaySettingsWidget::create_wallpaper_list()
{
    Core::DirIterator iterator("/res/wallpapers/", Core::DirIterator::Flags::SkipDots);

    m_wallpapers.append("Use background color");

    while (iterator.has_next()) {
        m_wallpapers.append(iterator.next_path());
    }

    m_modes.append("simple");
    m_modes.append("tile");
    m_modes.append("center");
    m_modes.append("stretch");
}

void DisplaySettingsWidget::create_frame()
{
    load_from_gml(display_settings_window_gml);

    m_monitor_widget = *find_descendant_of_type_named<DisplaySettings::MonitorWidget>("monitor_widget");

    m_wallpaper_combo = *find_descendant_of_type_named<GUI::ComboBox>("wallpaper_combo");
    m_wallpaper_combo->set_only_allow_values_from_model(true);
    m_wallpaper_combo->set_model(*GUI::ItemListModel<String>::create(m_wallpapers));
    m_wallpaper_combo->on_change = [this](auto& text, const GUI::ModelIndex& index) {
        String path = text;
        if (path.starts_with("/") && m_monitor_widget->set_wallpaper(path)) {
            m_monitor_widget->update();
            return;
        }

        if (index.row() == 0) {
            path = "";
        } else {
            if (index.is_valid()) {
                StringBuilder builder;
                builder.append("/res/wallpapers/");
                builder.append(path);
                path = builder.to_string();
            }
        }

        m_monitor_widget->set_wallpaper(path);
        m_monitor_widget->update();
    };

    auto& button = *find_descendant_of_type_named<GUI::Button>("wallpaper_open_button");
    button.set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"));
    button.on_click = [this](auto) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(nullptr, "Select wallpaper from file system.");

        if (!open_path.has_value())
            return;

        m_wallpaper_combo->set_only_allow_values_from_model(false);
        m_wallpaper_combo->set_text(open_path.value());
        m_wallpaper_combo->set_only_allow_values_from_model(true);
    };

    m_mode_combo = *find_descendant_of_type_named<GUI::ComboBox>("mode_combo");
    m_mode_combo->set_only_allow_values_from_model(true);
    m_mode_combo->set_model(*GUI::ItemListModel<String>::create(m_modes));
    m_mode_combo->on_change = [this](auto&, const GUI::ModelIndex& index) {
        m_monitor_widget->set_wallpaper_mode(m_modes.at(index.row()));
        m_monitor_widget->update();
    };

    m_resolution_combo = *find_descendant_of_type_named<GUI::ComboBox>("resolution_combo");
    m_resolution_combo->set_only_allow_values_from_model(true);
    m_resolution_combo->set_model(*GUI::ItemListModel<Gfx::IntSize>::create(m_resolutions));
    m_resolution_combo->on_change = [this](auto&, const GUI::ModelIndex& index) {
        m_monitor_widget->set_desktop_resolution(m_resolutions.at(index.row()));
        m_monitor_widget->update();
    };

    m_display_scale_radio_1x = *find_descendant_of_type_named<GUI::RadioButton>("scale_1x");
    m_display_scale_radio_1x->on_checked = [this](bool checked) {
        if (checked) {
            m_monitor_widget->set_desktop_scale_factor(1);
            m_monitor_widget->update();
        }
    };
    m_display_scale_radio_2x = *find_descendant_of_type_named<GUI::RadioButton>("scale_2x");
    m_display_scale_radio_2x->on_checked = [this](bool checked) {
        if (checked) {
            m_monitor_widget->set_desktop_scale_factor(2);
            m_monitor_widget->update();
        }
    };

    m_color_input = *find_descendant_of_type_named<GUI::ColorInput>("color_input");
    m_color_input->set_color_has_alpha_channel(false);
    m_color_input->set_color_picker_title("Select color for desktop");
    m_color_input->on_change = [this] {
        m_monitor_widget->set_background_color(m_color_input->color());
        m_monitor_widget->update();
    };

    auto& ok_button = *find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [this](auto) {
        send_settings_to_window_server();
        GUI::Application::the()->quit();
    };

    auto& cancel_button = *find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.on_click = [](auto) {
        GUI::Application::the()->quit();
    };

    auto& apply_button = *find_descendant_of_type_named<GUI::Button>("apply_button");
    apply_button.on_click = [this](auto) {
        send_settings_to_window_server();
    };
}

void DisplaySettingsWidget::load_current_settings()
{
    auto ws_config(Core::ConfigFile::open("/etc/WindowServer/WindowServer.ini"));
    auto wm_config = Core::ConfigFile::get_for_app("WindowManager");

    /// Wallpaper path ////////////////////////////////////////////////////////////////////////////
    /// Read wallpaper path from config file and set value to monitor widget and combo box.
    auto selected_wallpaper = wm_config->read_entry("Background", "Wallpaper", "");
    if (!selected_wallpaper.is_empty()) {
        m_monitor_widget->set_wallpaper(selected_wallpaper);

        Optional<size_t> optional_index;
        if (selected_wallpaper.starts_with("/res/wallpapers/")) {
            auto name_parts = selected_wallpaper.split('/', true);
            optional_index = m_wallpapers.find_first_index(name_parts[2]);

            if (optional_index.has_value()) {
                m_wallpaper_combo->set_selected_index(optional_index.value());
            }
        }

        if (!optional_index.has_value()) {
            m_wallpaper_combo->set_only_allow_values_from_model(false);
            m_wallpaper_combo->set_text(selected_wallpaper);
            m_wallpaper_combo->set_only_allow_values_from_model(true);
        }
    } else {
        m_wallpaper_combo->set_selected_index(0);
    }

    size_t index;

    /// Mode //////////////////////////////////////////////////////////////////////////////////////
    auto mode = ws_config->read_entry("Background", "Mode", "simple");
    if (!m_modes.contains_slow(mode)) {
        warnln("Invalid background mode '{}' in WindowServer config, falling back to 'simple'", mode);
        mode = "simple";
    }
    m_monitor_widget->set_wallpaper_mode(mode);
    index = m_modes.find_first_index(mode).value();
    m_mode_combo->set_selected_index(index);

    /// Resolution and scale factor ///////////////////////////////////////////////////////////////
    int scale_factor = ws_config->read_num_entry("Screen", "ScaleFactor", 1);
    if (scale_factor != 1 && scale_factor != 2) {
        dbgln("unexpected ScaleFactor {}, setting to 1", scale_factor);
        scale_factor = 1;
    }
    (scale_factor == 1 ? m_display_scale_radio_1x : m_display_scale_radio_2x)->set_checked(true);
    m_monitor_widget->set_desktop_scale_factor(scale_factor);

    // Let's attempt to find the current resolution and select it!
    Gfx::IntSize find_size;
    find_size.set_width(ws_config->read_num_entry("Screen", "Width", 1024));
    find_size.set_height(ws_config->read_num_entry("Screen", "Height", 768));
    index = m_resolutions.find_first_index(find_size).value_or(0);
    Gfx::IntSize m_current_resolution = m_resolutions.at(index);
    m_monitor_widget->set_desktop_resolution(m_current_resolution);
    m_resolution_combo->set_selected_index(index);

    /// Color /////////////////////////////////////////////////////////////////////////////////////
    /// If presend read from config file. If not paint with palette color.
    Color palette_desktop_color = palette().desktop_background();

    auto background_color = ws_config->read_entry("Background", "Color", "");
    if (!background_color.is_empty()) {
        auto opt_color = Color::from_string(background_color);
        if (opt_color.has_value())
            palette_desktop_color = opt_color.value();
    }

    m_color_input->set_color(palette_desktop_color);
    m_monitor_widget->set_background_color(palette_desktop_color);

    m_monitor_widget->update();
}

void DisplaySettingsWidget::send_settings_to_window_server()
{
    // Store the current screen resolution and scale factor in case the user wants to revert to it.
    auto ws_config(Core::ConfigFile::open("/etc/WindowServer/WindowServer.ini"));
    Gfx::IntSize current_resolution;
    current_resolution.set_width(ws_config->read_num_entry("Screen", "Width", 1024));
    current_resolution.set_height(ws_config->read_num_entry("Screen", "Height", 768));
    int current_scale_factor = ws_config->read_num_entry("Screen", "ScaleFactor", 1);
    if (current_scale_factor != 1 && current_scale_factor != 2) {
        dbgln("unexpected ScaleFactor {}, setting to 1", current_scale_factor);
        current_scale_factor = 1;
    }

    if (current_resolution != m_monitor_widget->desktop_resolution() || current_scale_factor != m_monitor_widget->desktop_scale_factor()) {
        auto result = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetResolution>(m_monitor_widget->desktop_resolution(), m_monitor_widget->desktop_scale_factor());
        if (!result->success()) {
            GUI::MessageBox::show(nullptr, String::formatted("Reverting to resolution {}x{} @ {}x", result->resolution().width(), result->resolution().height(), result->scale_factor()),
                "Unable to set resolution", GUI::MessageBox::Type::Error);
        } else {
            auto box = GUI::MessageBox::construct(window(), String::formatted("Do you want to keep the new settings? They will be reverted after 10 seconds."),
                String::formatted("New screen resolution: {}x{} @ {}x", m_monitor_widget->desktop_resolution().width(), m_monitor_widget->desktop_resolution().height(),
                    m_monitor_widget->desktop_scale_factor()),
                GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
            box->set_icon(window()->icon());

            // If after 10 seconds the user doesn't close the message box, just close it.
            auto timer = Core::Timer::construct(10000, [&] {
                box->close();
            });

            // If the user selects "No", closes the window or the window gets closed by the 10 seconds timer, revert the changes.
            if (box->exec() != GUI::MessageBox::ExecYes) {
                result = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetResolution>(current_resolution, current_scale_factor);
                if (!result->success()) {
                    GUI::MessageBox::show(nullptr, String::formatted("Reverting to resolution {}x{} @ {}x", result->resolution().width(), result->resolution().height(), result->scale_factor()),
                        "Unable to set resolution", GUI::MessageBox::Type::Error);
                }
            }
        }
    }

    if (!m_monitor_widget->wallpaper().is_empty()) {
        GUI::Desktop::the().set_wallpaper(m_monitor_widget->wallpaper());
    } else {
        dbgln("Setting color input: __{}__", m_color_input->text());
        GUI::Desktop::the().set_wallpaper("");
        GUI::Desktop::the().set_background_color(m_color_input->text());
    }

    GUI::Desktop::the().set_wallpaper_mode(m_monitor_widget->wallpaper_mode());
}
