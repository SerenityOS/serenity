/*
 * Copyright (c) 2021, Fabian Blatz <fabianblatz@gmail.com>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
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
#include <LibGUI/Application.h>
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

static ByteString sanitize_name(ByteString const& name)
{
    return name.replace(" "sv, ""sv).replace("="sv, ""sv);
}

static ByteString entry_to_config_string(size_t index, NonnullOwnPtr<QuickLaunchEntry> const& entry)
{
    return ByteString::formatted("{}:{}", index, entry->path());
}

OwnPtr<QuickLaunchEntry> QuickLaunchEntry::create_from_path(StringView path)
{
    if (path.ends_with(".af"sv)) {
        auto af_path = path.to_byte_string();
        if (!path.starts_with('/'))
            af_path = ByteString::formatted("{}/{}", Desktop::AppFile::APP_FILES_DIRECTORY, path);

        return make<QuickLaunchEntryAppFile>(Desktop::AppFile::open(af_path));
    }

    auto stat_or_error = Core::System::stat(path);
    if (stat_or_error.is_error()) {
        dbgln("Failed to stat quick launch entry file: {}", stat_or_error.release_error());
        return {};
    }

    auto stat = stat_or_error.release_value();
    if (S_ISREG(stat.st_mode) && ((stat.st_mode & (S_IXUSR | S_IXGRP | S_IXOTH)) != 0))
        return make<QuickLaunchEntryExecutable>(path);
    return make<QuickLaunchEntryFile>(path);
}

ErrorOr<void> QuickLaunchEntryAppFile::launch() const
{
    TRY(m_app_file->spawn_with_escalation());
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

ByteString QuickLaunchEntryExecutable::name() const
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
ErrorOr<NonnullRefPtr<QuickLaunchWidget>> QuickLaunchWidget::create()
{
    auto widget = TRY(AK::adopt_nonnull_ref_or_enomem(new (nothrow) QuickLaunchWidget()));
    TRY(widget->create_context_menu());
    widget->load_entries();
    return widget;
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

        auto executable = process_object.get_byte_string("executable"sv);
        if (!executable.has_value())
            break;

        auto maybe_name = process_object.get_byte_string("name"sv);
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

        TRY(update_entry(name, new_entry.release_nonnull()));
        return true;
    }

    return false;
}

void QuickLaunchWidget::config_key_was_removed(StringView domain, StringView group, StringView key)
{
    if (domain == "Taskbar" && group == CONFIG_GROUP_ENTRIES)
        remove_entry(key, false);
}

void QuickLaunchWidget::config_string_did_change(StringView domain, StringView group, StringView, StringView)
{
    if (domain == "Taskbar" && group == CONFIG_GROUP_ENTRIES)
        load_entries(false);
}

void QuickLaunchWidget::drag_enter_event(GUI::DragEvent& event)
{
    if (event.mime_data().has_urls())
        event.accept();
}

void QuickLaunchWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        for (auto& url : urls) {
            auto path = URL::percent_decode(url.serialize_path());
            auto entry = QuickLaunchEntry::create_from_path(path);
            if (entry) {
                auto entry_name = entry->name();
                auto result = update_entry(entry_name, entry.release_nonnull());
                if (result.is_error())
                    GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to add quick launch entry: {}", result.release_error()));
            }
        }
    }
}

void QuickLaunchWidget::mousedown_event(GUI::MouseEvent& event)
{
    for_each_entry([&](NonnullOwnPtr<QuickLaunchEntry> const& entry, Gfx::IntRect rect) {
        if (m_dragging && !entry->is_pressed())
            return;

        entry->set_pressed(rect.contains(event.position()));
        if (entry->is_pressed())
            m_grab_offset = rect.x() - event.x();
    });
    update();
}

void QuickLaunchWidget::mousemove_event(GUI::MouseEvent& event)
{
    m_mouse_pos = event.position();
    for_each_entry([&](NonnullOwnPtr<QuickLaunchEntry> const& entry, Gfx::IntRect rect) {
        entry->set_hovered(rect.contains(event.position()));
        if (entry->is_pressed())
            m_dragging = true;

        if (entry->is_hovered())
            GUI::Application::the()->show_tooltip(String::from_byte_string(entry->name()).release_value_but_fixme_should_propagate_errors(), this);
    });

    if (m_dragging)
        recalculate_order();

    update();
}

void QuickLaunchWidget::mouseup_event(GUI::MouseEvent& event)
{
    for_each_entry([&](NonnullOwnPtr<QuickLaunchEntry> const& entry, Gfx::IntRect) {
        if (!m_dragging && entry->is_pressed() && event.button() == GUI::MouseButton::Primary) {
            auto result = entry->launch();
            if (result.is_error()) {
                // FIXME: This message box is displayed in a weird position
                GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to open quick launch entry: {}", result.release_error()));
            }
        }

        entry->set_pressed(false);
    });

    m_dragging = false;

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

    m_dragging = false;
    m_grab_offset = 0;

    update();
    event.accept();
    Widget::leave_event(event);
}

void QuickLaunchWidget::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);

    GUI::Painter painter(*this);

    auto paint_entry = [this, &painter](NonnullOwnPtr<QuickLaunchEntry> const& entry, Gfx::IntRect rect) {
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
    };

    NonnullOwnPtr<QuickLaunchEntry> const* dragged_entry = nullptr;
    Gfx::IntRect dragged_entry_rect;

    for_each_entry([&](NonnullOwnPtr<QuickLaunchEntry> const& entry, Gfx::IntRect rect) {
        if (m_dragging && entry->is_pressed()) {
            rect.set_x(m_mouse_pos.x() + m_grab_offset);
            dragged_entry = &entry;
            dragged_entry_rect = rect;
            return;
        }

        paint_entry(entry, rect);
    });

    if (dragged_entry)
        paint_entry(*dragged_entry, dragged_entry_rect);
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
        remove_entry(m_context_menu_app_name);
        repaint();
    });
    m_context_menu->add_action(*m_context_menu_default_action);

    return {};
}

void QuickLaunchWidget::load_entries(bool save)
{
    struct ConfigEntry {
        int index;
        ByteString path;
    };

    Vector<ConfigEntry> config_entries;
    auto keys = Config::list_keys(CONFIG_DOMAIN, CONFIG_GROUP_ENTRIES);
    for (auto& name : keys) {
        auto value = Config::read_string(CONFIG_DOMAIN, CONFIG_GROUP_ENTRIES, name);
        auto values = value.split(':');

        config_entries.append({ values[0].to_number<int>().release_value(), values[1] });
    }

    quick_sort(config_entries, [](ConfigEntry const& a, ConfigEntry const& b) {
        return a.index < b.index;
    });

    Vector<NonnullOwnPtr<QuickLaunchEntry>> entries;
    for (auto const& config_entry : config_entries) {
        auto entry = QuickLaunchEntry::create_from_path(config_entry.path);
        if (!entry)
            continue;

        entries.append(entry.release_nonnull());
    }

    // backwards compatibility since the group and value-format changed
    auto old_keys = Config::list_keys(CONFIG_DOMAIN, OLD_CONFIG_GROUP_ENTRIES);
    if (!old_keys.is_empty()) {
        for (auto& name : old_keys) {
            auto path = Config::read_string(CONFIG_DOMAIN, OLD_CONFIG_GROUP_ENTRIES, name);
            auto entry = QuickLaunchEntry::create_from_path(path);
            if (!entry)
                continue;

            entries.append(entry.release_nonnull());
        }

        Config::remove_group(CONFIG_DOMAIN, OLD_CONFIG_GROUP_ENTRIES);
    }

    m_entries.clear();
    add_entries(move(entries), save);
}

void QuickLaunchWidget::add_entries(Vector<NonnullOwnPtr<QuickLaunchEntry>> entries, bool save)
{
    size_t size = entries.size();
    for (size_t i = 0; i < size; i++) {
        m_entries.append(entries.take(0));
        if (save)
            Config::write_string(CONFIG_DOMAIN, CONFIG_GROUP_ENTRIES, sanitize_name(m_entries.last()->name()), entry_to_config_string(m_entries.size() - 1, m_entries.last()));
    }

    repaint();
}

ErrorOr<void> QuickLaunchWidget::update_entry(ByteString const& button_name, NonnullOwnPtr<QuickLaunchEntry> entry, bool save)
{
    auto file_name_to_watch = entry->file_name_to_watch();
    if (!file_name_to_watch.is_empty()) {
        if (!m_watcher) {
            m_watcher = TRY(Core::FileWatcher::create());
            m_watcher->on_change = [button_name, save, this](Core::FileWatcherEvent const&) {
                dbgln("Removing QuickLaunch entry \"{}\"", button_name);
                remove_entry(button_name, save);
                repaint();
            };
        }
        TRY(m_watcher->add_watch(file_name_to_watch, Core::FileWatcherEvent::Type::Deleted));
    }

    set_or_insert_entry(move(entry), save);
    repaint();

    return {};
}

template<typename Callback>
void QuickLaunchWidget::for_each_entry(Callback callback)
{
    Gfx::IntRect rect(0, 0, BUTTON_SIZE, BUTTON_SIZE);
    for (auto const& entry : m_entries) {
        callback(entry, rect);
        rect.translate_by(BUTTON_SIZE, 0);
    }
}

void QuickLaunchWidget::resize()
{
    set_fixed_width(static_cast<int>(m_entries.size()) * BUTTON_SIZE);
}

void QuickLaunchWidget::repaint()
{
    resize();
    update();
}

void QuickLaunchWidget::set_or_insert_entry(NonnullOwnPtr<QuickLaunchEntry> entry, bool save)
{
    auto name = entry->name();
    for (size_t i = 0; i < m_entries.size(); i++) {
        auto& value = m_entries[i];
        if (value->name() != name)
            continue;
        value = move(entry);
        if (save)
            Config::write_string(CONFIG_DOMAIN, CONFIG_GROUP_ENTRIES, sanitize_name(value->name()), entry_to_config_string(i, value));
        return;
    }

    if (save)
        Config::write_string(CONFIG_DOMAIN, CONFIG_GROUP_ENTRIES, sanitize_name(entry->name()), entry_to_config_string(m_entries.size(), entry));
    m_entries.append(move(entry));
}

void QuickLaunchWidget::remove_entry(ByteString const& name, bool save)
{
    for (size_t i = 0; i < m_entries.size(); i++) {
        if (m_entries[i]->name() != name)
            continue;
        if (save)
            Config::remove_key(CONFIG_DOMAIN, CONFIG_GROUP_ENTRIES, m_entries[i]->name());
        m_entries.remove(i);
        return;
    }
}
void QuickLaunchWidget::recalculate_order()
{
    if (!m_dragging)
        return;

    size_t dragged_index = 0;
    for (; dragged_index < m_entries.size(); dragged_index++) {
        if (m_entries[dragged_index]->is_pressed())
            break;
    }

    size_t new_index = m_entries.size() + 1;
    Gfx::IntRect rect(0, 0, BUTTON_SIZE, BUTTON_SIZE);
    for (size_t i = 0; i < m_entries.size(); i++) {
        auto left_break_point = i == 0 ? rect.x() + rect.width() / 2 : rect.x();
        if (m_mouse_pos.x() < left_break_point) {
            new_index = i;
            break;
        }

        if (i == m_entries.size() - 1 && m_mouse_pos.x() > rect.x() + rect.width() / 2) {
            new_index = i + 1;
            break;
        }

        rect.translate_by(BUTTON_SIZE, 0);
    }

    if (new_index >= m_entries.size() + 1 || new_index == dragged_index)
        return;

    if (dragged_index < new_index)
        new_index--;

    auto entry = m_entries.take(dragged_index);
    m_entries.insert(new_index, move(entry));

    for (size_t i = 0; i < m_entries.size(); i++)
        Config::write_string(CONFIG_DOMAIN, CONFIG_GROUP_ENTRIES, sanitize_name(m_entries[i]->name()), entry_to_config_string(i, m_entries[i]));
}

}
