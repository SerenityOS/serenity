/*
 * Copyright (c) 2021, Fabian Blatz <fabianblatz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "QuickLaunchWidget.h"
#include <AK/LexicalPath.h>
#include <Kernel/API/InodeWatcherFlags.h>
#include <LibConfig/Client.h>
#include <LibCore/FileWatcher.h>
#include <LibCore/MimeData.h>
#include <LibCore/Process.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <serenity.h>
#include <sys/stat.h>

namespace Taskbar {

constexpr auto quick_launch = "QuickLaunch"sv;
constexpr int quick_launch_button_size = 24;

ErrorOr<void> QuickLaunchEntryAppFile::launch() const
{
    auto executable = m_app_file->executable();

    pid_t pid = TRY(Core::System::fork());
    if (pid == 0) {
        if (chdir(Core::StandardPaths::home_directory().characters()) < 0) {
            perror("chdir");
            exit(1);
        }
        if (m_app_file->run_in_terminal())
            execl("/bin/Terminal", "Terminal", "-e", executable.characters(), nullptr);
        else
            execl(executable.characters(), executable.characters(), nullptr);
        perror("execl");
        VERIFY_NOT_REACHED();
    } else
        TRY(Core::System::disown(pid));
    return {};
}

ErrorOr<void> QuickLaunchEntryExecutable::launch() const
{
    auto pid = Core::Process::spawn(m_path);
    if (pid < 0)
        return Error::from_syscall("Core::Process::spawn", -errno);
    return {};
}

GUI::Icon QuickLaunchEntryExecutable::icon() const
{
    return GUI::FileIconProvider::icon_for_executable(m_path);
}

String QuickLaunchEntryExecutable::name() const
{
    return LexicalPath { m_path }.basename();
}

ErrorOr<void> QuickLaunchEntryFile::launch() const
{
    if (!Desktop::Launcher::open(URL::create_with_url_or_path(m_path))) {
        // FIXME: LaunchServer doesn't inform us about errors
        return Error::from_string_literal("Failed to open file");
    }
    return {};
}

GUI::Icon QuickLaunchEntryFile::icon() const
{
    return GUI::FileIconProvider::icon_for_path(m_path);
}

String QuickLaunchEntryFile::name() const
{
    // '=' is a special character in config files
    return m_path;
}

QuickLaunchWidget::QuickLaunchWidget()
{
    set_shrink_to_fit(true);
    set_layout<GUI::HorizontalBoxLayout>();
    layout()->set_spacing(0);
    set_frame_thickness(0);
    set_fixed_height(24);

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
        auto value = Config::read_string("Taskbar", quick_launch, name);
        auto entry = QuickLaunchEntry::create_from_config_value(value);
        if (!entry)
            continue;
        add_or_adjust_button(name, entry.release_nonnull());
    }
}

QuickLaunchWidget::~QuickLaunchWidget()
{
}

OwnPtr<QuickLaunchEntry> QuickLaunchEntry::create_from_config_value(StringView value)
{
    if (!value.starts_with("/") && value.ends_with(".af")) {
        auto af_path = String::formatted("{}/{}", Desktop::AppFile::APP_FILES_DIRECTORY, value);
        return make<QuickLaunchEntryAppFile>(Desktop::AppFile::open(af_path));
    }
    return create_from_path(value);
}

OwnPtr<QuickLaunchEntry> QuickLaunchEntry::create_from_path(StringView path)
{
    if (path.ends_with(".af"))
        return make<QuickLaunchEntryAppFile>(Desktop::AppFile::open(path));
    auto stat_or_error = Core::System::stat(path);
    if (stat_or_error.is_error()) {
        dbgln("Failed to stat quick launch entry file: {}", stat_or_error.release_error());
        return {};
    }

    auto stat = stat_or_error.release_value();
    if (S_ISREG(stat.st_mode) && (stat.st_mode & (S_IXUSR | S_IXGRP | S_IXOTH)))
        return make<QuickLaunchEntryExecutable>(path);
    return make<QuickLaunchEntryFile>(path);
}

static String sanitize_entry_name(String const& name)
{
    return name.replace(" ", "", true).replace("=", "", true);
}

void QuickLaunchWidget::add_or_adjust_button(String const& button_name, NonnullOwnPtr<QuickLaunchEntry>&& entry)
{
    auto file_name_to_watch = entry->file_name_to_watch();
    if (!file_name_to_watch.is_null()) {
        if (!m_watcher) {
            // FIXME: Propagate errors
            m_watcher = MUST(Core::FileWatcher::create());
            m_watcher->on_change = [this](Core::FileWatcherEvent const& event) {
                auto name = sanitize_entry_name(event.event_path);
                dbgln("Removing QuickLaunch entry {}", name);
                auto button = find_child_of_type_named<GUI::Button>(name);
                if (button)
                    remove_child(*button);
            };
        }
        // FIXME: Propagate errors
        MUST(m_watcher->add_watch(file_name_to_watch, Core::FileWatcherEvent::Type::Deleted));
    }

    auto button = find_child_of_type_named<GUI::Button>(button_name);
    if (!button)
        button = &add<GUI::Button>();

    button->set_fixed_size(quick_launch_button_size, quick_launch_button_size);
    button->set_button_style(Gfx::ButtonStyle::Coolbar);
    auto icon = entry->icon();
    button->set_icon(icon.bitmap_for_size(16));
    button->set_tooltip(entry->name());
    button->set_name(button_name);
    button->on_click = [entry = move(entry), this](auto) {
        auto result = entry->launch();
        if (result.is_error()) {
            // FIXME: This message box is displayed in a weird position
            GUI::MessageBox::show_error(window(), String::formatted("Failed to open quick launch entry: {}", result.release_error()));
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
        auto entry = QuickLaunchEntry::create_from_config_value(value);
        if (!entry)
            return;
        add_or_adjust_button(key, entry.release_nonnull());
    }
}

void QuickLaunchWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        for (auto& url : urls) {
            auto entry = QuickLaunchEntry::create_from_path(url.path());
            if (entry) {
                auto item_name = sanitize_entry_name(entry->name());
                add_or_adjust_button(item_name, entry.release_nonnull());
                Config::write_string("Taskbar", quick_launch, item_name, url.path());
            }
        }
    }
}

}
