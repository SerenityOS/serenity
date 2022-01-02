/*
 * Copyright (c) 2021, Fabian Blatz <fabianblatz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "QuickLaunchWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/MimeData.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Menu.h>
#include <LibGfx/Alignment.h>
#include <serenity.h>

namespace Taskbar {

constexpr auto quick_launch = "QuickLaunch"sv;
constexpr int quick_launch_button_size = 24;

QuickLaunchWidget::QuickLaunchWidget(Gfx::Orientation orientation)
{
    set_shrink_to_fit(true);
    if (orientation == Gfx::Orientation::Horizontal) {
        set_layout<GUI::HorizontalBoxLayout>();
        set_fixed_height(24);
    } else {
        set_layout<GUI::VerticalBoxLayout>();
        set_fixed_width(38);
    }

    layout()->set_spacing(0);
    set_frame_thickness(0);

    m_context_menu = GUI::Menu::construct();
    m_context_menu_default_action = GUI::Action::create("&Remove", [this](auto&) {
        Config::remove_key("Taskbar", quick_launch, m_context_menu_app_name);
        auto button = find_child_of_type_named<GUI::Button>(m_context_menu_app_name);
        if (button) {
            remove_child(*button);
        }
    });
    m_context_menu->add_action(*m_context_menu_default_action);

    auto keys = Config::list_keys("Taskbar", quick_launch);
    for (auto& name : keys) {
        auto af_name = Config::read_string("Taskbar", quick_launch, name);
        auto af_path = String::formatted("{}/{}", Desktop::AppFile::APP_FILES_DIRECTORY, af_name);
        auto af = Desktop::AppFile::open(af_path);
        add_or_adjust_button(name, af);
    }
}

QuickLaunchWidget::~QuickLaunchWidget()
{
}

void QuickLaunchWidget::add_or_adjust_button(String const& button_name, NonnullRefPtr<Desktop::AppFile> app_file)
{
    if (!app_file->is_valid())
        return;
    auto button = find_child_of_type_named<GUI::Button>(button_name);
    if (!button) {
        button = &add<GUI::Button>();
    }
    auto app_executable = app_file->executable();
    auto app_run_in_terminal = app_file->run_in_terminal();
    button->set_fixed_size(quick_launch_button_size, quick_launch_button_size);
    button->set_button_style(Gfx::ButtonStyle::Coolbar);
    button->set_icon(app_file->icon().bitmap_for_size(16));
    button->set_tooltip(app_file->name());
    button->set_name(button_name);
    button->on_click = [app_executable, app_run_in_terminal](auto) {
        pid_t pid = fork();
        if (pid < 0) {
            perror("fork");
        } else if (pid == 0) {
            if (chdir(Core::StandardPaths::home_directory().characters()) < 0) {
                perror("chdir");
                exit(1);
            }
            if (app_run_in_terminal)
                execl("/bin/Terminal", "Terminal", "-e", app_executable.characters(), nullptr);
            else
                execl(app_executable.characters(), app_executable.characters(), nullptr);
            perror("execl");
            VERIFY_NOT_REACHED();
        } else {
            if (disown(pid) < 0)
                perror("disown");
        }
    };
    button->on_context_menu_request = [this, button_name](auto& context_menu_event) {
        m_context_menu_app_name = button_name;
        m_context_menu->popup(context_menu_event.screen_position(), m_context_menu_default_action);
    };
}

void QuickLaunchWidget::config_key_was_removed(String const& domain, String const& group, String const& key)
{
    if (domain == "Taskbar" && group == quick_launch) {
        auto button = find_child_of_type_named<GUI::Button>(key);
        if (button)
            remove_child(*button);
    }
}

void QuickLaunchWidget::config_string_did_change(String const& domain, String const& group, String const& key, String const& value)
{
    if (domain == "Taskbar" && group == quick_launch) {
        auto af_path = String::formatted("{}/{}", Desktop::AppFile::APP_FILES_DIRECTORY, value);
        auto af = Desktop::AppFile::open(af_path);
        add_or_adjust_button(key, af);
    }
}

void QuickLaunchWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        for (auto& url : urls) {
            auto af = Desktop::AppFile::open(url.path());
            if (af->is_valid()) {
                auto item_name = af->name().replace(" ", "", true);
                add_or_adjust_button(item_name, af);
                Config::write_string("Taskbar", quick_launch, item_name, url.basename());
            }
        }
    }
}

}
