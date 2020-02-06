/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibCore/CDirIterator.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GFileSystemModel.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GListView.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GTabWidget.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWindowServerConnection.h>
#include <Servers/WindowServer/WSWindowManager.h>

DisplayPropertiesWidget::DisplayPropertiesWidget()
    : m_wm_config(Core::ConfigFile::get_for_app("WindowManager"))
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
    m_root_widget->set_layout(make<GUI::VBoxLayout>());
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
    auto tab_widget = GUI::TabWidget::construct(m_root_widget);

    // First, let's create the "Background" tab
    auto background_splitter = GUI::Splitter::construct(Orientation::Vertical, nullptr);
    tab_widget->add_widget("Wallpaper", background_splitter);

    auto background_content = GUI::Widget::construct(background_splitter.ptr());
    background_content->set_layout(make<GUI::VBoxLayout>());
    background_content->layout()->set_margins({ 4, 4, 4, 4 });

    m_wallpaper_preview = GUI::Label::construct(background_splitter);

    auto wallpaper_list = GUI::ListView::construct(background_content);
    wallpaper_list->set_background_color(Color::White);
    wallpaper_list->set_model(*ItemListModel<AK::String>::create(m_wallpapers));

    auto wallpaper_model = wallpaper_list->model();
    auto find_first_wallpaper_index = m_wallpapers.find_first_index(m_selected_wallpaper);
    auto wallpaper_index_in_model = wallpaper_model->index(find_first_wallpaper_index, wallpaper_list->model_column());
    if (wallpaper_model->is_valid(wallpaper_index_in_model))
        wallpaper_list->selection().set(wallpaper_index_in_model);

    wallpaper_list->horizontal_scrollbar().set_visible(false);
    wallpaper_list->on_selection = [this](auto& index) {
        StringBuilder builder;
        m_selected_wallpaper = m_wallpapers.at(index.row());
        builder.append("/res/wallpapers/");
        builder.append(m_selected_wallpaper);
        m_wallpaper_preview->set_icon(Gfx::Bitmap::load_from_file(builder.to_string()));
        m_wallpaper_preview->set_should_stretch_icon(true);
    };

    // Let's add the settings tab
    auto settings_splitter = GUI::Splitter::construct(Orientation::Vertical, nullptr);
    tab_widget->add_widget("Settings", settings_splitter);

    auto settings_content = GUI::Widget::construct(settings_splitter.ptr());
    settings_content->set_layout(make<GUI::VBoxLayout>());
    settings_content->layout()->set_margins({ 4, 4, 4, 4 });

    auto resolution_list = GUI::ListView::construct(settings_content);
    resolution_list->set_background_color(Color::White);
    resolution_list->set_model(*ItemListModel<Gfx::Size>::create(m_resolutions));

    auto resolution_model = resolution_list->model();
    auto find_first_resolution_index = m_resolutions.find_first_index(m_selected_resolution);
    auto resolution_index_in_model = resolution_model->index(find_first_resolution_index, resolution_list->model_column());
    if (resolution_model->is_valid(resolution_index_in_model))
        resolution_list->selection().set(resolution_index_in_model);

    resolution_list->horizontal_scrollbar().set_visible(false);
    resolution_list->on_selection = [this](auto& index) {
        m_selected_resolution = m_resolutions.at(index.row());
    };

    settings_content->layout()->add_spacer();

    // Add the apply and cancel buttons
    auto bottom_widget = GUI::Widget::construct(m_root_widget.ptr());
    bottom_widget->set_layout(make<GUI::HBoxLayout>());
    bottom_widget->layout()->add_spacer();
    bottom_widget->set_size_policy(Orientation::Vertical, GUI::SizePolicy::Fixed);
    bottom_widget->set_preferred_size(1, 22);

    auto apply_button = GUI::Button::construct(bottom_widget);
    apply_button->set_text("Apply");
    apply_button->set_size_policy(Orientation::Vertical, GUI::SizePolicy::Fixed);
    apply_button->set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    apply_button->set_preferred_size(60, 22);
    apply_button->on_click = [this, tab_widget](GUI::Button&) {
        send_settings_to_window_server(tab_widget->active_tab_index());
    };

    auto ok_button = GUI::Button::construct(bottom_widget);
    ok_button->set_text("OK");
    ok_button->set_size_policy(Orientation::Vertical, GUI::SizePolicy::Fixed);
    ok_button->set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    ok_button->set_preferred_size(60, 22);
    ok_button->on_click = [this, tab_widget](GUI::Button&) {
        send_settings_to_window_server(tab_widget->active_tab_index());
        GUI::Application::the().quit();
    };

    auto cancel_button = GUI::Button::construct(bottom_widget);
    cancel_button->set_text("Cancel");
    cancel_button->set_size_policy(Orientation::Vertical, GUI::SizePolicy::Fixed);
    cancel_button->set_size_policy(Orientation::Horizontal, GUI::SizePolicy::Fixed);
    cancel_button->set_preferred_size(60, 22);
    cancel_button->on_click = [](auto&) {
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
        GUI::WindowServerConnection::the().send_sync<WindowServer::SetResolution>(m_selected_resolution);
    } else {
        dbg() << "Invalid tab index " << tab_index;
    }
}
