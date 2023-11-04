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
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>
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
    widget->add_quick_launch_buttons(move(entries));
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
        remove_entry(m_context_menu_app_name);
        resize();
        update();
    });
    m_context_menu->add_action(*m_context_menu_default_action);

    return {};
}

void QuickLaunchWidget::add_quick_launch_buttons(Vector<NonnullOwnPtr<QuickLaunchEntry>> entries)
{
    size_t size = entries.size();
    for (size_t i = 0; i < size; i++)
        m_entries.append(entries.take(0));

    resize();
    update();
}

OwnPtr<QuickLaunchEntry> QuickLaunchEntry::create_from_config_value(StringView path)
{
    if (!path.starts_with('/') && path.ends_with(".af"sv)) {
        auto af_path = DeprecatedString::formatted("{}/{}", Desktop::AppFile::APP_FILES_DIRECTORY, path);
        return make<QuickLaunchEntryAppFile>(Desktop::AppFile::open(af_path));
    }
    return create_from_path(path);
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

ErrorOr<void> QuickLaunchWidget::add_or_adjust_button(DeprecatedString const& button_name, NonnullOwnPtr<QuickLaunchEntry> entry)
{
    auto file_name_to_watch = entry->file_name_to_watch();
    if (!file_name_to_watch.is_empty()) {
        if (!m_watcher) {
            m_watcher = TRY(Core::FileWatcher::create());
            m_watcher->on_change = [button_name, this](Core::FileWatcherEvent const& event) {
                auto name = sanitize_entry_name(event.event_path);
                dbgln("Removing QuickLaunch entry {}", name);
                remove_entry(button_name);
                resize();
                update();
            };
        }
        TRY(m_watcher->add_watch(file_name_to_watch, Core::FileWatcherEvent::Type::Deleted));
    }

    set_or_insert_entry(move(entry));
    resize();
    update();

    return {};
}

void QuickLaunchWidget::config_key_was_removed(StringView domain, StringView group, StringView key)
{
    if (domain == "Taskbar" && group == quick_launch)
        remove_entry(key);
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
    if (mime_types.contains_slow("text/uri-list"sv))
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

void QuickLaunchWidget::mousedown_event(GUI::MouseEvent& event)
{
    for_each_entry([&](NonnullOwnPtr<QuickLaunchEntry> const& entry, Gfx::IntRect rect) {
        entry->set_pressed(rect.contains(event.position()));
    });
    update();
}

void QuickLaunchWidget::mousemove_event(GUI::MouseEvent& event)
{
    for_each_entry([&](NonnullOwnPtr<QuickLaunchEntry> const& entry, Gfx::IntRect rect) {
        entry->set_hovered(rect.contains(event.position()));
    });
    update();
}

void QuickLaunchWidget::mouseup_event(GUI::MouseEvent& event)
{
    for_each_entry([&](NonnullOwnPtr<QuickLaunchEntry> const& entry, Gfx::IntRect) {
        if (entry->is_pressed() && event.button() == GUI::MouseButton::Primary) {
            auto result = entry->launch();
            if (result.is_error()) {
                // FIXME: This message box is displayed in a weird position
                GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to open quick launch entry: {}", result.release_error()));
            }
        }

        entry->set_pressed(false);
    });

    update();
}

void QuickLaunchWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    for_each_entry([&](NonnullOwnPtr<QuickLaunchEntry> const& entry, Gfx::IntRect rect) {
        if (!rect.contains(event.position()))
            return;

        m_context_menu_app_name = entry->name();
        m_context_menu->popup(event.screen_position(), m_context_menu_default_action);
    });
}

void QuickLaunchWidget::leave_event(Core::Event& event)
{
    for_each_entry([&](NonnullOwnPtr<QuickLaunchEntry> const& entry, auto) {
        entry->set_pressed(false);
        entry->set_hovered(false);
    });

    update();
    event.accept();
    Widget::leave_event(event);
}

void QuickLaunchWidget::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);

    GUI::Painter painter(*this);

    for_each_entry([&](NonnullOwnPtr<QuickLaunchEntry> const& entry, Gfx::IntRect rect) {
        Gfx::StylePainter::paint_button(painter, rect, palette(), Gfx::ButtonStyle::Coolbar, entry->is_pressed(), entry->is_hovered());

        auto const* icon = entry->icon().bitmap_for_size(16);
        auto content_rect = rect.shrunken(8, 2);
        auto icon_location = content_rect.center().translated(-(icon->width() / 2), -(icon->height() / 2));
        if (entry->is_pressed())
            icon_location.translate_by(1, 1);

        if (entry->is_hovered())
            painter.blit_brightened(icon_location, *icon, icon->rect());
        else
            painter.blit(icon_location, *icon, icon->rect());
    });
}

template<typename Callback>
void QuickLaunchWidget::for_each_entry(Callback callback)
{
    Gfx::IntRect rect(0, 0, quick_launch_button_size, quick_launch_button_size);
    for (auto const& entry : m_entries) {
        callback(entry, rect);
        rect.translate_by(quick_launch_button_size, 0);
    }
}

ErrorOr<bool> QuickLaunchWidget::add_from_pid(pid_t pid_to_add)
{
    auto processes_file = TRY(Core::File::open("/sys/kernel/processes"sv, Core::File::OpenMode::Read));
    auto file_content = TRY(processes_file->read_until_eof());
    auto json_obj = TRY(JsonValue::from_string(file_content)).as_object();
    for (auto value : json_obj.get_array("processes"sv).release_value().values()) {
        auto& process_object = value.as_object();
        auto pid = process_object.get_i32("pid"sv).value_or(0);
        if (pid != pid_to_add)
            continue;

        auto executable = process_object.get_deprecated_string("executable"sv);
        if (!executable.has_value())
            break;

        auto maybe_name = process_object.get_deprecated_string("name"sv);
        if (!maybe_name.has_value())
            break;

        auto name = maybe_name.release_value();
        auto path = executable.release_value();
        if (Desktop::AppFile::exists_for_app(name)) {
            path = Desktop::AppFile::app_file_path_for_app(name);
        }

        auto new_entry = QuickLaunchEntry::create_from_path(path);
        if (!new_entry)
            break;

        TRY(add_or_adjust_button(name, new_entry.release_nonnull()));
        return true;
    }

    return false;
}

void QuickLaunchWidget::resize()
{
    set_fixed_width(m_entries.size() * quick_launch_button_size);
}

void QuickLaunchWidget::set_or_insert_entry(NonnullOwnPtr<QuickLaunchEntry> entry)
{
    auto name = entry->name();
    for (auto& value : m_entries) {
        if (value->name() != name)
            continue;
        value = move(entry);
        return;
    }

    m_entries.append(move(entry));
}

void QuickLaunchWidget::remove_entry(DeprecatedString const& name)
{
    for (size_t i = 0; i < m_entries.size(); i++) {
        if (m_entries[i]->name() != name)
            continue;
        m_entries.remove(i);
        return;
    }
}

}
