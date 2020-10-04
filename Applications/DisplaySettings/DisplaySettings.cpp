/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
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
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>

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
    m_resolutions.append({ 1280, 1024 });
    m_resolutions.append({ 1360, 768 });
    m_resolutions.append({ 1368, 768 });
    m_resolutions.append({ 1440, 900 });
    m_resolutions.append({ 1600, 900 });
    m_resolutions.append({ 1920, 1080 });
    m_resolutions.append({ 2560, 1080 });
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
    m_modes.append("scaled");
}

void DisplaySettingsWidget::create_frame()
{
    m_root_widget = GUI::Widget::construct();
    m_root_widget->set_layout<GUI::VerticalBoxLayout>();
    m_root_widget->set_fill_with_background_color(true);
    m_root_widget->layout()->set_margins({ 4, 4, 4, 4 });

    auto& settings_content = m_root_widget->add<GUI::Widget>();
    settings_content.set_layout<GUI::VerticalBoxLayout>();
    settings_content.set_backcolor("red");
    settings_content.set_background_color(Color::Blue);
    settings_content.set_background_role(Gfx::ColorRole::Window);
    settings_content.layout()->set_margins({ 4, 4, 4, 4 });

    /// Wallpaper Preview /////////////////////////////////////////////////////////////////////////

    m_monitor_widget = settings_content.add<MonitorWidget>();
    m_monitor_widget->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_monitor_widget->set_preferred_size(338, 248);

    /// Wallpaper Row /////////////////////////////////////////////////////////////////////////////

    auto& wallpaper_selection_container = settings_content.add<GUI::Widget>();
    wallpaper_selection_container.set_layout<GUI::HorizontalBoxLayout>();
    wallpaper_selection_container.layout()->set_margins({ 0, 4, 0, 0 });
    wallpaper_selection_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    wallpaper_selection_container.set_preferred_size(0, 22);

    auto& wallpaper_label = wallpaper_selection_container.add<GUI::Label>();
    wallpaper_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    wallpaper_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    wallpaper_label.set_preferred_size({ 70, 0 });
    wallpaper_label.set_text("Wallpaper:");

    m_wallpaper_combo = wallpaper_selection_container.add<GUI::ComboBox>();
    m_wallpaper_combo->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_wallpaper_combo->set_preferred_size(0, 22);
    m_wallpaper_combo->set_only_allow_values_from_model(true);
    m_wallpaper_combo->set_model(*GUI::ItemListModel<AK::String>::create(m_wallpapers));
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

    auto& button = wallpaper_selection_container.add<GUI::Button>();
    button.set_tooltip("Select Wallpaper from file system.");
    button.set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"));
    button.set_button_style(Gfx::ButtonStyle::CoolBar);
    button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    button.set_preferred_size(22, 22);
    button.on_click = [this](auto) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(root_widget()->window(), "Select wallpaper from file system.");

        if (!open_path.has_value())
            return;

        m_wallpaper_combo->set_only_allow_values_from_model(false);
        this->m_wallpaper_combo->set_text(open_path.value());
        m_wallpaper_combo->set_only_allow_values_from_model(true);
    };

    /// Mode //////////////////////////////////////////////////////////////////////////////////////

    auto& mode_selection_container = settings_content.add<GUI::Widget>();
    mode_selection_container.set_layout<GUI::HorizontalBoxLayout>();
    mode_selection_container.layout()->set_margins({ 0, 4, 0, 0 });
    mode_selection_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    mode_selection_container.set_preferred_size(0, 22);

    auto& mode_label = mode_selection_container.add<GUI::Label>();
    mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    mode_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    mode_label.set_preferred_size({ 70, 0 });
    mode_label.set_text("Mode:");

    m_mode_combo = mode_selection_container.add<GUI::ComboBox>();
    m_mode_combo->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_mode_combo->set_preferred_size(0, 22);
    m_mode_combo->set_only_allow_values_from_model(true);
    m_mode_combo->set_model(*GUI::ItemListModel<AK::String>::create(m_modes));
    m_mode_combo->on_change = [this](auto&, const GUI::ModelIndex& index) {
        this->m_monitor_widget->set_wallpaper_mode(m_modes.at(index.row()));
        this->m_monitor_widget->update();
    };

    /// Resolution Row ////////////////////////////////////////////////////////////////////////////

    auto& resolution_selection_container = settings_content.add<GUI::Widget>();
    resolution_selection_container.set_layout<GUI::HorizontalBoxLayout>();
    resolution_selection_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    resolution_selection_container.set_preferred_size(0, 22);

    auto& m_resolution_label = resolution_selection_container.add<GUI::Label>();
    m_resolution_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    m_resolution_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_resolution_label.set_preferred_size({ 70, 0 });
    m_resolution_label.set_text("Resolution:");

    m_resolution_combo = resolution_selection_container.add<GUI::ComboBox>();
    m_resolution_combo->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_resolution_combo->set_preferred_size(0, 22);
    m_resolution_combo->set_only_allow_values_from_model(true);
    m_resolution_combo->set_model(*GUI::ItemListModel<Gfx::IntSize>::create(m_resolutions));
    m_resolution_combo->on_change = [this](auto&, const GUI::ModelIndex& index) {
        this->m_monitor_widget->set_desktop_resolution(m_resolutions.at(index.row()));
        this->m_monitor_widget->update();
    };

    /// Background Color Row //////////////////////////////////////////////////////////////////////

    auto& color_selection_container = settings_content.add<GUI::Widget>();
    color_selection_container.set_layout<GUI::HorizontalBoxLayout>();
    color_selection_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    color_selection_container.set_preferred_size(0, 22);

    auto& color_label = color_selection_container.add<GUI::Label>();
    color_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    color_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    color_label.set_preferred_size({ 70, 0 });
    color_label.set_text("Color:");

    m_color_input = color_selection_container.add<GUI::ColorInput>();
    m_color_input->set_color_has_alpha_channel(false);
    m_color_input->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_color_input->set_preferred_size(90, 0);
    m_color_input->set_color_picker_title("Select color for desktop");
    m_color_input->on_change = [this] {
        this->m_monitor_widget->set_background_color(m_color_input->color());
        this->m_monitor_widget->update();
    };

    /// Add the apply and cancel buttons //////////////////////////////////////////////////////////

    auto& bottom_widget = settings_content.add<GUI::Widget>();
    bottom_widget.set_layout<GUI::HorizontalBoxLayout>();
    bottom_widget.layout()->add_spacer();
    //bottom_widget.layout()->set_margins({ 4, 10, 4, 10 });
    bottom_widget.set_size_policy(Orientation::Vertical, GUI::SizePolicy::Fixed);
    bottom_widget.set_preferred_size(1, 22);

    auto& ok_button = bottom_widget.add<GUI::Button>();
    ok_button.set_text("OK");
    ok_button.set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    ok_button.set_preferred_size(60, 22);
    ok_button.on_click = [this](auto) {
        send_settings_to_window_server();
        GUI::Application::the()->quit();
    };

    auto& cancel_button = bottom_widget.add<GUI::Button>();
    cancel_button.set_text("Cancel");
    cancel_button.set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    cancel_button.set_preferred_size(60, 22);
    cancel_button.on_click = [](auto) {
        GUI::Application::the()->quit();
    };

    auto& apply_button = bottom_widget.add<GUI::Button>();
    apply_button.set_text("Apply");
    apply_button.set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    apply_button.set_preferred_size(60, 22);
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

    /// Mode //////////////////////////////////////////////////////////////////////////////////////
    auto mode = ws_config->read_entry("Background", "Mode");
    if (!mode.is_empty()) {
        this->m_monitor_widget->set_wallpaper_mode(mode);
        auto index = m_modes.find_first_index(mode).value();
        m_mode_combo->set_selected_index(index);
    }

    /// Resolution ////////////////////////////////////////////////////////////////////////////////
    Gfx::IntSize find_size;

    // Let's attempt to find the current resolution and select it!
    find_size.set_width(ws_config->read_num_entry("Screen", "Width", 1024));
    find_size.set_height(ws_config->read_num_entry("Screen", "Height", 768));

    size_t index = m_resolutions.find_first_index(find_size).value_or(0);
    Gfx::IntSize m_current_resolution = m_resolutions.at(index);
    m_monitor_widget->set_desktop_resolution(m_current_resolution);
    m_resolution_combo->set_selected_index(index);

    /// Color /////////////////////////////////////////////////////////////////////////////////////
    /// If presend read from config file. If not paint with palet color.
    Color palette_desktop_color = this->palette().desktop_background();

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
    auto result = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetResolution>(m_monitor_widget->desktop_resolution());
    if (!result->success()) {
        GUI::MessageBox::show(root_widget()->window(), String::formatted("Reverting to resolution {}x{}", result->resolution().width(), result->resolution().height()),
            "Unable to set resolution", GUI::MessageBox::Type::Error);
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
