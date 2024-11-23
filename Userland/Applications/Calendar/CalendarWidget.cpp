/*
 * Copyright (c) 2023, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CalendarWidget.h"
#include "AddEventDialog.h"
#include "ViewEventDialog.h"
#include <AK/JsonParser.h>
#include <AK/LexicalPath.h>
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Calendar.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Process.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

namespace Calendar {

ErrorOr<NonnullRefPtr<CalendarWidget>> CalendarWidget::create(GUI::Window* parent_window)
{
    auto widget = TRY(CalendarWidget::try_create());

    widget->m_event_calendar = widget->find_descendant_of_type_named<EventCalendar>("calendar");
    widget->create_on_events_change();

    auto toolbar = widget->find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    auto calendar = widget->m_event_calendar;

    auto prev_date_action = TRY(widget->create_prev_date_action());
    auto next_date_action = TRY(widget->create_next_date_action());

    auto add_event_action = TRY(widget->create_add_event_action());

    auto jump_to_action = TRY(widget->create_jump_to_action());

    auto view_month_action = TRY(widget->create_view_month_action());
    view_month_action->set_checked(true);

    auto view_year_action = TRY(widget->create_view_year_action());

    widget->m_view_type_action_group = make<GUI::ActionGroup>();
    widget->m_view_type_action_group->set_exclusive(true);
    widget->m_view_type_action_group->add_action(*view_month_action);
    widget->m_view_type_action_group->add_action(*view_year_action);

    auto default_view = Config::read_string("Calendar"sv, "View"sv, "DefaultView"sv, "Month"sv);
    if (default_view == "Year")
        view_year_action->set_checked(true);

    auto open_settings_action = TRY(widget->create_open_settings_action());

    toolbar->add_action(prev_date_action);
    toolbar->add_action(next_date_action);
    toolbar->add_separator();
    toolbar->add_action(jump_to_action);
    toolbar->add_action(add_event_action);
    toolbar->add_separator();
    toolbar->add_action(view_month_action);
    toolbar->add_action(view_year_action);
    toolbar->add_action(open_settings_action);

    widget->create_on_tile_doubleclick();

    calendar->on_month_click = [view_month_action] {
        view_month_action->set_checked(true);
    };

    auto new_calendar_action = TRY(widget->create_new_calendar_action());
    auto open_calendar_action = widget->create_open_calendar_action();

    auto save_as_action = widget->create_save_as_action();
    auto save_action = widget->create_save_action(save_as_action);

    auto file_menu = parent_window->add_menu("&File"_string);
    file_menu->add_action(open_settings_action);
    file_menu->add_action(new_calendar_action);
    file_menu->add_action(open_calendar_action);
    file_menu->add_action(save_as_action);
    file_menu->add_action(save_action);

    file_menu->add_separator();

    file_menu->add_action(GUI::CommonActions::make_quit_action([widget](auto&) {
        if (!widget->request_close())
            return;
        GUI::Application::the()->quit();
    }));

    widget->m_save_action = save_action;

    auto event_menu = parent_window->add_menu("&Event"_string);
    event_menu->add_action(add_event_action);

    auto view_menu = parent_window->add_menu("&View"_string);
    view_menu->add_action(*view_month_action);
    view_menu->add_action(*view_year_action);

    view_menu->add_separator();
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        parent_window->set_fullscreen(!parent_window->is_fullscreen());
    }));

    auto help_menu = parent_window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(parent_window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/Calendar.md"sv), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Calendar"_string, TRY(GUI::Icon::try_create_default_icon("app-calendar"sv)), parent_window));

    return widget;
}

bool CalendarWidget::request_close()
{
    if (!m_event_calendar->event_manager().is_dirty())
        return true;

    auto result = GUI::MessageBox::ask_about_unsaved_changes(window(), m_event_calendar->event_manager().current_filename());
    if (result == GUI::MessageBox::ExecResult::Yes) {
        m_save_action->activate();
        return !m_event_calendar->event_manager().is_dirty();
    }

    if (result == GUI::MessageBox::ExecResult::No)
        return true;

    return false;
}

void CalendarWidget::create_on_events_change()
{
    m_event_calendar->event_manager().on_events_change = [&]() {
        m_event_calendar->repaint();
        window()->set_modified(true);
        update_window_title();
    };
}

void CalendarWidget::load_file(FileSystemAccessClient::File file)
{
    auto result = m_event_calendar->event_manager().load_file(file);
    if (result.is_error()) {
        GUI::MessageBox::show_error(window(), ByteString::formatted("Cannot load file: {}", result.error()));
        return;
    }

    window()->set_modified(false);
    update_window_title();
}

NonnullRefPtr<GUI::Action> CalendarWidget::create_save_action(GUI::Action& save_as_action)
{
    return GUI::CommonActions::make_save_action([&](auto&) {
        if (current_filename().is_empty()) {
            save_as_action.activate();
            return;
        }

        auto response = FileSystemAccessClient::Client::the().request_file(window(), current_filename(), Core::File::OpenMode::Write);
        if (response.is_error())
            return;

        auto result = m_event_calendar->event_manager().save(response.value());
        if (result.is_error()) {
            GUI::MessageBox::show_error(window(), ByteString::formatted("Cannot save file: {}", result.error()));
            return;
        }

        window()->set_modified(false);
        update_window_title();
    });
}

NonnullRefPtr<GUI::Action> CalendarWidget::create_save_as_action()
{
    return GUI::CommonActions::make_save_as_action([&](auto&) {
        auto response = FileSystemAccessClient::Client::the().save_file(window(), "calendar", "cal");
        if (response.is_error())
            return;

        auto result = m_event_calendar->event_manager().save(response.value());
        if (result.is_error()) {
            GUI::MessageBox::show_error(window(), ByteString::formatted("Cannot save file: {}", result.error()));
            return;
        }

        window()->set_modified(false);
        update_window_title();
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> CalendarWidget::create_new_calendar_action()
{
    return GUI::Action::create("&New Calendar", {}, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-calendar.png"sv)), [&](const GUI::Action&) {
        auto response = FileSystemAccessClient::Client::the().save_file(window(), "calendar", "cal", Core::File::OpenMode::Write);

        if (response.is_error())
            return;

        m_event_calendar->event_manager().clear();

        auto result = m_event_calendar->event_manager().save(response.value());
        if (result.is_error()) {
            GUI::MessageBox::show_error(window(), ByteString::formatted("Cannot save file: {}", result.error()));
            return;
        }

        update_window_title();
    });
}

NonnullRefPtr<GUI::Action> CalendarWidget::create_open_calendar_action()
{
    return GUI::CommonActions::make_open_action([&](auto&) {
        GUI::FileTypeFilter calendar_files;
        calendar_files.name = "Calendar Files";
        calendar_files.extensions = Vector<ByteString> { "cal", "ics" };
        auto response = FileSystemAccessClient::Client::the().open_file(window(), { .allowed_file_types = Vector { calendar_files, GUI::FileTypeFilter::all_files() } });
        if (response.is_error())
            return;
        (void)load_file(response.release_value());
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> CalendarWidget::create_prev_date_action()
{
    return GUI::Action::create({}, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"sv)), [&](const GUI::Action&) {
        m_event_calendar->show_previous_date();
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> CalendarWidget::create_next_date_action()
{
    return GUI::Action::create({}, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"sv)), [&](const GUI::Action&) {
        m_event_calendar->show_next_date();
    });
}

void CalendarWidget::update_window_title()
{
    StringBuilder builder;
    if (current_filename().is_empty())
        builder.append("Untitled"sv);
    else
        builder.append(current_filename());
    builder.append("[*] - Calendar"sv);

    window()->set_title(builder.to_byte_string());
}

ErrorOr<NonnullRefPtr<GUI::Action>> CalendarWidget::create_add_event_action()
{
    return GUI::Action::create("&Add Event", {}, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/add-event.png"sv)), [&](const GUI::Action&) {
        AddEventDialog::show(m_event_calendar->selected_date(), m_event_calendar->event_manager(), window());
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> CalendarWidget::create_jump_to_action()
{
    return GUI::Action::create("Jump to &Today", {}, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/calendar-date.png"sv)), [&](const GUI::Action&) {
        m_event_calendar->set_selected_date(Core::DateTime::now());
        m_event_calendar->update_tiles(Core::DateTime::now().year(), Core::DateTime::now().month());
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> CalendarWidget::create_view_month_action()
{
    return GUI::Action::create_checkable("&Month View", { Mod_Ctrl, KeyCode::Key_1 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/calendar-month-view.png"sv)), [&](const GUI::Action&) {
        if (m_event_calendar->mode() == GUI::Calendar::Year)
            m_event_calendar->toggle_mode();
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> CalendarWidget::create_view_year_action()
{
    return GUI::Action::create_checkable("&Year View", { Mod_Ctrl, KeyCode::Key_2 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/icon-view.png"sv)), [&](const GUI::Action&) {
        if (m_event_calendar->mode() == GUI::Calendar::Month)
            m_event_calendar->toggle_mode();
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> CalendarWidget::create_open_settings_action()
{
    return GUI::Action::create("Calendar &Settings", {}, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-settings.png"sv)), [&](GUI::Action const&) {
        GUI::Process::spawn_or_show_error(window(), "/bin/CalendarSettings"sv);
    });
}

void CalendarWidget::create_on_tile_doubleclick()
{
    m_event_calendar->on_tile_doubleclick = [&] {
        for (auto const& event : m_event_calendar->event_manager().events()) {
            auto start = event.start;
            auto selected_date = m_event_calendar->selected_date();

            if (start.year() == selected_date.year() && start.month() == selected_date.month() && start.day() == selected_date.day()) {
                ViewEventDialog::show(selected_date, m_event_calendar->event_manager(), window());
                return;
            }
        }

        AddEventDialog::show(m_event_calendar->selected_date(), m_event_calendar->event_manager(), window());
    };
}
}
