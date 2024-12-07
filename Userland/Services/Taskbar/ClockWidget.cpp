/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClockWidget.h"
#include <LibConfig/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Process.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>

namespace Taskbar {

ClockWidget::ClockWidget()
{
    set_frame_style(Gfx::FrameStyle::SunkenPanel);

    update_format(Config::read_string("Taskbar"sv, "Clock"sv, "TimeFormat"sv, "%T"sv));

    m_timer = add<Core::Timer>(1000, [this] {
        static time_t last_update_time;
        time_t now = time(nullptr);
        if (now != last_update_time) {
            tick_clock();
            last_update_time = now;
            set_tooltip(MUST(Core::DateTime::now().to_string("%Y-%m-%d"sv)));
        }
    });
    m_timer->start();

    m_calendar_window = add<GUI::Window>(window());
    m_calendar_window->set_window_type(GUI::WindowType::Popup);
    m_calendar_window->resize(m_window_size.width(), m_window_size.height());

    auto root_container = m_calendar_window->set_main_widget<GUI::Frame>();
    root_container->set_fill_with_background_color(true);
    root_container->set_layout<GUI::VerticalBoxLayout>(GUI::Margins { 2, 0 }, 0);
    root_container->set_frame_style(Gfx::FrameStyle::Window);

    auto& navigation_container = root_container->add<GUI::Widget>();
    navigation_container.set_fixed_height(24);
    navigation_container.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins { 2 });

    m_prev_date = navigation_container.add<GUI::Button>();
    m_prev_date->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_prev_date->set_fixed_size(24, 24);
    m_prev_date->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"sv).release_value_but_fixme_should_propagate_errors());
    m_prev_date->on_click = [&](auto) {
        m_calendar->show_previous_date();
        update_selected_calendar_button();
    };

    m_selected_calendar_button = navigation_container.add<GUI::Button>();
    m_selected_calendar_button->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_selected_calendar_button->set_fixed_height(24);
    m_selected_calendar_button->on_click = [&](auto) {
        m_calendar->toggle_mode();
        update_selected_calendar_button();
    };

    m_next_date = navigation_container.add<GUI::Button>();
    m_next_date->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_next_date->set_fixed_size(24, 24);
    m_next_date->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"sv).release_value_but_fixme_should_propagate_errors());
    m_next_date->on_click = [&](auto) {
        m_calendar->show_next_date();
        update_selected_calendar_button();
    };

    auto& separator1 = root_container->add<GUI::HorizontalSeparator>();
    separator1.set_fixed_height(2);

    auto& calendar_container = root_container->add<GUI::Widget>();
    calendar_container.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins { 2 });

    m_calendar = calendar_container.add<GUI::Calendar>();
    m_selected_calendar_button->set_text(m_calendar->formatted_date().release_value_but_fixme_should_propagate_errors());

    m_calendar->on_scroll = [&] {
        update_selected_calendar_button();
    };

    m_calendar->on_tile_click = [&] {
        m_selected_calendar_button->set_text(m_calendar->formatted_date().release_value_but_fixme_should_propagate_errors());
    };

    m_calendar->on_month_click = [&] {
        m_selected_calendar_button->set_text(m_calendar->formatted_date().release_value_but_fixme_should_propagate_errors());
    };

    auto& separator2 = root_container->add<GUI::HorizontalSeparator>();
    separator2.set_fixed_height(2);

    auto& settings_container = root_container->add<GUI::Widget>();
    settings_container.set_fixed_height(24);
    settings_container.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins { 2 });
    settings_container.add_spacer();

    m_jump_to_button = settings_container.add<GUI::Button>();
    m_jump_to_button->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_jump_to_button->set_fixed_size(24, 24);
    m_jump_to_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/calendar-date.png"sv).release_value_but_fixme_should_propagate_errors());
    m_jump_to_button->set_tooltip("Jump to today"_string);
    m_jump_to_button->on_click = [this](auto) {
        jump_to_current_date();
    };

    m_calendar_launcher = settings_container.add<GUI::Button>();
    m_calendar_launcher->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_calendar_launcher->set_fixed_size(24, 24);
    m_calendar_launcher->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-calendar.png"sv).release_value_but_fixme_should_propagate_errors());
    m_calendar_launcher->set_tooltip("Calendar"_string);
    m_calendar_launcher->on_click = [this](auto) {
        GUI::Process::spawn_or_show_error(window(), "/bin/Calendar"sv);
    };
}

void ClockWidget::update_format(ByteString const& format)
{
    m_time_format = format;
    m_time_width = font().width(Core::DateTime::create(122, 2, 22, 22, 22, 22).to_byte_string(format));
    set_fixed_size(m_time_width + 20, 21);
}

void ClockWidget::paint_event(GUI::PaintEvent& event)
{
    TaskbarFrame::paint_event(event);
    auto time_text = Core::DateTime::now().to_byte_string(m_time_format);
    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());

    // Render string center-left aligned, but attempt to center the string based on a constant
    // "ideal" time string (i.e., the same one used to size this widget in the initializer).
    // This prevents the rest of the string from shifting around while seconds tick.
    Gfx::Font const& font = Gfx::FontDatabase::default_font();
    int const frame_width = frame_thickness();
    int const ideal_width = m_time_width;
    int const widget_width = max_width().as_int();
    int const translation_x = (widget_width - ideal_width) / 2 - frame_width;

    painter.draw_text(frame_inner_rect().translated(translation_x, frame_width), time_text, font, Gfx::TextAlignment::CenterLeft, palette().window_text());
}

void ClockWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary) {
        return;
    } else {
        if (!m_calendar_window->is_visible())
            open();
        else
            close();
    }
}

void ClockWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = GUI::Menu::construct();

        auto settings_icon = MUST(Gfx::Bitmap::load_from_file("/res/icons/16x16/settings.png"sv));
        auto open_clock_settings_action = GUI::Action::create("Clock &Settings", *settings_icon, [this](auto&) {
            GUI::Process::spawn_or_show_error(window(), "/bin/ClockSettings"sv, Array { "--open-tab", "clock" });
        });

        m_context_menu->add_action(open_clock_settings_action);
    }

    m_context_menu->popup(event.screen_position());
}

void ClockWidget::open()
{
    jump_to_current_date();
    position_calendar_window();
    m_calendar_window->show();
}

void ClockWidget::close()
{
    m_calendar_window->hide();
}

void ClockWidget::position_calendar_window()
{
    constexpr auto taskbar_top_padding { 4 };
    m_calendar_window->set_rect(
        screen_relative_rect().right() - m_calendar_window->width(),
        screen_relative_rect().top() - taskbar_top_padding - m_calendar_window->height(),
        m_window_size.width(),
        m_window_size.height());
}

void ClockWidget::jump_to_current_date()
{
    if (m_calendar->mode() == GUI::Calendar::Year)
        m_calendar->toggle_mode();
    m_calendar->set_selected_date(Core::DateTime::now());
    m_calendar->update_tiles(Core::DateTime::now().year(), Core::DateTime::now().month());
    m_selected_calendar_button->set_text(m_calendar->formatted_date().release_value_but_fixme_should_propagate_errors());
}

void ClockWidget::update_selected_calendar_button()
{
    if (m_calendar->mode() == GUI::Calendar::Year)
        m_selected_calendar_button->set_text(m_calendar->formatted_date(GUI::Calendar::YearOnly).release_value_but_fixme_should_propagate_errors());
    else
        m_selected_calendar_button->set_text(m_calendar->formatted_date().release_value_but_fixme_should_propagate_errors());
}

}
