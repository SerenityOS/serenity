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

#include "DisplayProperties.h"
#include "ItemListModel.h"
#include <AK/StringBuilder.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/ListView.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <Servers/WindowServer/WindowManager.h>

DisplayPropertiesWidget::DisplayPropertiesWidget()
    : m_wm_config(Core::ConfigFile::open("/etc/WindowServer/WindowServer.ini"))
{
    create_resolution_list();
    create_wallpaper_list();
    create_root_widget();
    create_frame();
}

void DisplayPropertiesWidget::create_resolution_list()
{
    // TODO: Find a better way to get the default resolution
    m_resolutions.append({ 640, 480 });
    m_resolutions.append({ 800, 600 });
    m_resolutions.append({ 1024, 768 });
    m_resolutions.append({ 1280, 720 });
    m_resolutions.append({ 1368, 768 });
    m_resolutions.append({ 1366, 768 });
    m_resolutions.append({ 1280, 1024 });
    m_resolutions.append({ 1440, 900 });
    m_resolutions.append({ 1600, 900 });
    m_resolutions.append({ 1920, 1080 });
    m_resolutions.append({ 2560, 1080 });

    Gfx::Size find_size;

    bool okay = false;
    // Let's attempt to find the current resolution and select it!
    find_size.set_width(m_wm_config->read_entry("Screen", "Width", "1024").to_int(okay));
    if (!okay) {
        fprintf(stderr, "DisplayProperties: failed to convert width to int!");
        return;
    }

    find_size.set_height(m_wm_config->read_entry("Screen", "Height", "768").to_int(okay));
    if (!okay) {
        fprintf(stderr, "DisplayProperties: failed to convert height to int!");
        return;
    }

    int index = 0;
    for (auto& resolution : m_resolutions) {
        if (resolution == find_size) {
            m_selected_resolution = m_resolutions.at(index);
            return; // We don't need to do anything else
        }

        index++;
    }

    m_selected_resolution = m_resolutions.at(0);
}

void DisplayPropertiesWidget::create_root_widget()
{
    m_root_widget = GUI::Widget::construct();
    m_root_widget->set_layout<GUI::VerticalBoxLayout>();
    m_root_widget->set_fill_with_background_color(true);
    m_root_widget->layout()->set_margins({ 4, 4, 4, 16 });
}

void DisplayPropertiesWidget::create_wallpaper_list()
{
    m_selected_wallpaper = m_wm_config->read_entry("Background", "Wallpaper");
    if (!m_selected_wallpaper.is_empty()) {
        auto name_parts = m_selected_wallpaper.split('/');
        m_selected_wallpaper = name_parts[2];
    }

    Core::DirIterator iterator("/res/wallpapers/", Core::DirIterator::Flags::SkipDots);

    while (iterator.has_next()) {
        m_wallpapers.append(iterator.next_path());
    }
}

void DisplayPropertiesWidget::create_frame()
{
    auto& tab_widget = m_root_widget->add<GUI::TabWidget>();

    auto wallpaper_splitter = tab_widget.add_tab<GUI::VerticalSplitter>("Wallpaper");

    auto& wallpaper_content = wallpaper_splitter->add<GUI::Widget>();
    wallpaper_content.set_layout<GUI::VerticalBoxLayout>();
    wallpaper_content.layout()->set_margins({ 4, 4, 4, 4 });

    m_wallpaper_preview = wallpaper_splitter->add<GUI::Label>();

    auto& wallpaper_list = wallpaper_content.add<GUI::ListView>();
    wallpaper_list.set_background_color(Color::White);
    wallpaper_list.set_model(*ItemListModel<AK::String>::create(m_wallpapers));

    auto wallpaper_model = wallpaper_list.model();
    auto find_first_wallpaper_index = m_wallpapers.find_first_index(m_selected_wallpaper);
    if (find_first_wallpaper_index.has_value()) {
        auto wallpaper_index_in_model = wallpaper_model->index(find_first_wallpaper_index.value(), wallpaper_list.model_column());
        if (wallpaper_model->is_valid(wallpaper_index_in_model))
            wallpaper_list.selection().set(wallpaper_index_in_model);
    }

    wallpaper_list.horizontal_scrollbar().set_visible(false);
    wallpaper_list.on_selection = [this](auto& index) {
        StringBuilder builder;
        m_selected_wallpaper = m_wallpapers.at(index.row());
        builder.append("/res/wallpapers/");
        builder.append(m_selected_wallpaper);
        m_wallpaper_preview->set_icon(Gfx::Bitmap::load_from_file(builder.to_string()));
        m_wallpaper_preview->set_should_stretch_icon(true);
    };

    auto settings_splitter = tab_widget.add_tab<GUI::VerticalSplitter>("Settings");

    auto& settings_content = settings_splitter->add<GUI::Widget>();
    settings_content.set_layout<GUI::VerticalBoxLayout>();
    settings_content.layout()->set_margins({ 4, 4, 4, 4 });

    auto& resolution_list = settings_content.add<GUI::ListView>();
    resolution_list.set_background_color(Color::White);
    resolution_list.set_model(*ItemListModel<Gfx::Size>::create(m_resolutions));

    auto resolution_model = resolution_list.model();
    auto find_first_resolution_index = m_resolutions.find_first_index(m_selected_resolution);
    ASSERT(find_first_resolution_index.has_value());
    auto resolution_index_in_model = resolution_model->index(find_first_resolution_index.value(), resolution_list.model_column());
    if (resolution_model->is_valid(resolution_index_in_model))
        resolution_list.selection().set(resolution_index_in_model);

    resolution_list.horizontal_scrollbar().set_visible(false);
    resolution_list.on_selection = [this](auto& index) {
        m_selected_resolution = m_resolutions.at(index.row());
    };

    settings_content.layout()->add_spacer();

    // Add the apply and cancel buttons
    auto& bottom_widget = m_root_widget->add<GUI::Widget>();
    bottom_widget.set_layout<GUI::HorizontalBoxLayout>();
    bottom_widget.layout()->add_spacer();
    bottom_widget.set_size_policy(Orientation::Vertical, GUI::SizePolicy::Fixed);
    bottom_widget.set_preferred_size(1, 22);

    auto& apply_button = bottom_widget.add<GUI::Button>();
    apply_button.set_text("Apply");
    apply_button.set_size_policy(Orientation::Vertical, GUI::SizePolicy::Fixed);
    apply_button.set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    apply_button.set_preferred_size(60, 22);
    apply_button.on_click = [this, tab_widget = &tab_widget] {
        send_settings_to_window_server(tab_widget->active_tab_index());
    };

    auto& ok_button = bottom_widget.add<GUI::Button>();
    ok_button.set_text("OK");
    ok_button.set_size_policy(Orientation::Vertical, GUI::SizePolicy::Fixed);
    ok_button.set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    ok_button.set_preferred_size(60, 22);
    ok_button.on_click = [this, tab_widget = &tab_widget] {
        send_settings_to_window_server(tab_widget->active_tab_index());
        GUI::Application::the().quit();
    };

    auto& cancel_button = bottom_widget.add<GUI::Button>();
    cancel_button.set_text("Cancel");
    cancel_button.set_size_policy(Orientation::Vertical, GUI::SizePolicy::Fixed);
    cancel_button.set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    cancel_button.set_preferred_size(60, 22);
    cancel_button.on_click = [] {
        GUI::Application::the().quit();
    };
}

void DisplayPropertiesWidget::send_settings_to_window_server(int tab_index)
{
    if (tab_index == TabIndices::Wallpaper) {
        StringBuilder builder;
        builder.append("/res/wallpapers/");
        builder.append(m_selected_wallpaper);
        GUI::Desktop::the().set_wallpaper(builder.to_string());
    } else if (tab_index == TabIndices::Settings) {
        dbg() << "Attempting to set resolution " << m_selected_resolution;
        auto result = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetResolution>(m_selected_resolution);
        if (!result->success())
            GUI::MessageBox::show(String::format("Reverting to resolution %dx%d", result->resolution().width(), result->resolution().height()), String::format("Unable to set resolution"), GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK);
    } else {
        dbg() << "Invalid tab index " << tab_index;
    }
}
