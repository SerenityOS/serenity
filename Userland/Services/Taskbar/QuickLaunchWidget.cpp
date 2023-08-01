/*
 * Copyright (c) 2021, Fabian Blatz <fabianblatz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "QuickLaunchWidget.h"
#include <AK/LexicalPath.h>
#include <AK/OwnPtr.h>
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
    TRY(Core::Process::spawn(m_path));
    return {};
}

GUI::Icon QuickLaunchEntryExecutable::icon() const
{
    return GUI::FileIconProvider::icon_for_executable(m_path);
}

DeprecatedString QuickLaunchEntryExecutable::name() const
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

DeprecatedString QuickLaunchEntryFile::name() const
{
    // '=' is a special character in config files
    return m_path;
}

ErrorOr<NonnullRefPtr<QuickLaunchWidget>> QuickLaunchWidget::create()
{
    Vector<NonnullOwnPtr<QuickLaunchEntry>> entries;
    auto keys = Config::list_keys("Taskbar"sv, quick_launch);
    for (auto& name : keys) {
        auto value = Config::read_string("Taskbar"sv, quick_launch, name);
        auto entry = QuickLaunchEntry::create_from_config_value(value);
        if (!entry)
            continue;

        entries.append(entry.release_nonnull());
    }

    auto widget = TRY(AK::adopt_nonnull_ref_or_enomem(new (nothrow) QuickLaunchWidget()));
    TRY(widget->create_context_menu());
    TRY(widget->add_quick_launch_buttons(move(entries)));
    return widget;
}

QuickLaunchWidget::QuickLaunchWidget()
{
    set_shrink_to_fit(true);
    set_layout<GUI::HorizontalBoxLayout>(GUI::Margins {}, 0);
    set_frame_style(Gfx::FrameStyle::NoFrame);
    set_fixed_height(24);
}

ErrorOr<void> QuickLaunchWidget::create_context_menu()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/delete.png"sv));
    m_context_menu = GUI::Menu::construct();
    m_context_menu_default_action = GUI::Action::create("&Remove", icon, [this](auto&) {
        Config::remove_key("Taskbar"sv, quick_launch, m_context_menu_app_name);
        auto button = find_child_of_type_named<GUI::Button>(m_context_menu_app_name);
        if (button) {
            remove_child(*button);
        }
    });
    m_context_menu->add_action(*m_context_menu_default_action);

    return {};
}

ErrorOr<void> QuickLaunchWidget::add_quick_launch_buttons(Vector<NonnullOwnPtr<QuickLaunchEntry>> entries)
{
    for (auto& entry : entries) {
        auto name = entry->name();
        TRY(add_or_adjust_button(name, move(entry)));
    }

    return {};
}

OwnPtr<QuickLaunchEntry> QuickLaunchEntry::create_from_config_value(StringView value)
{
    if (!value.starts_with('/') && value.ends_with(".af"sv)) {
        auto af_path = DeprecatedString::formatted("{}/{}", Desktop::AppFile::APP_FILES_DIRECTORY, value);
        return make<QuickLaunchEntryAppFile>(Desktop::AppFile::open(af_path));
    }
    return create_from_path(value);
}

OwnPtr<QuickLaunchEntry> QuickLaunchEntry::create_from_path(StringView path)
{
    if (path.ends_with(".af"sv))
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

static DeprecatedString sanitize_entry_name(DeprecatedString const& name)
{
    return name.replace(" "sv, ""sv, ReplaceMode::All).replace("="sv, ""sv, ReplaceMode::All);
}

ErrorOr<void> QuickLaunchWidget::add_or_adjust_button(DeprecatedString const& button_name, NonnullOwnPtr<QuickLaunchEntry>&& entry)
{
    auto file_name_to_watch = entry->file_name_to_watch();
    if (!file_name_to_watch.is_null()) {
        if (!m_watcher) {
            m_watcher = TRY(Core::FileWatcher::create());
            m_watcher->on_change = [this](Core::FileWatcherEvent const& event) {
                auto name = sanitize_entry_name(event.event_path);
                dbgln("Removing QuickLaunch entry {}", name);
                auto button = find_child_of_type_named<GUI::Button>(name);
                if (button)
                    remove_child(*button);
            };
        }
        TRY(m_watcher->add_watch(file_name_to_watch, Core::FileWatcherEvent::Type::Deleted));
    }

    auto button = find_child_of_type_named<GUI::Button>(button_name);
    if (!button)
        button = &add<GUI::Button>();

    button->set_fixed_size(quick_launch_button_size, quick_launch_button_size);
    button->set_button_style(Gfx::ButtonStyle::Coolbar);
    auto icon = entry->icon();
    button->set_icon(icon.bitmap_for_size(16));
    button->set_tooltip_deprecated(entry->name());
    button->set_name(button_name);
    button->on_click = [entry = move(entry), this](auto) {
        auto result = entry->launch();
        if (result.is_error()) {
            // FIXME: This message box is displayed in a weird position
            GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to open quick launch entry: {}", result.release_error()));
        }
    };
    button->on_context_menu_request = [this, button_name](auto& context_menu_event) {
        m_context_menu_app_name = button_name;
        m_context_menu->popup(context_menu_event.screen_position(), m_context_menu_default_action);
    };

    return {};
}

void QuickLaunchWidget::config_key_was_removed(StringView domain, StringView group, StringView key)
{
    if (domain == "Taskbar" && group == quick_launch) {
        auto button = find_child_of_type_named<GUI::Button>(key);
        if (button)
            remove_child(*button);
    }
}

void QuickLaunchWidget::config_string_did_change(StringView domain, StringView group, StringView key, StringView value)
{
    if (domain == "Taskbar" && group == quick_launch) {
        auto entry = QuickLaunchEntry::create_from_config_value(value);
        if (!entry)
            return;
        auto result = add_or_adjust_button(key, entry.release_nonnull());
        if (result.is_error())
            GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to change quick launch entry: {}", result.release_error()));
    }
}

void QuickLaunchWidget::drag_enter_event(GUI::DragEvent& event)
{
    auto const& mime_types = event.mime_types();
    if (mime_types.contains_slow("text/uri-list"))
        event.accept();
}

void QuickLaunchWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        for (auto& url : urls) {
            auto path = url.serialize_path();
            auto entry = QuickLaunchEntry::create_from_path(path);
            if (entry) {
                auto item_name = sanitize_entry_name(entry->name());
                auto result = add_or_adjust_button(item_name, entry.release_nonnull());
                if (result.is_error())
                    GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to add quick launch entry: {}", result.release_error()));
                Config::write_string("Taskbar"sv, quick_launch, item_name, path);
            }
        }
    }
}

}
