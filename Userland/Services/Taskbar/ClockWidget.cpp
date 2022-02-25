/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClockWidget.h"
#include <LibCore/Process.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>

namespace Taskbar {

ClockWidget::ClockWidget()
{
    set_frame_shape(Gfx::FrameShape::Box);
    set_frame_shadow(Gfx::FrameShadow::Sunken);
    set_frame_thickness(1);

    m_time_width = font().width("22:22:22");

    set_fixed_size(m_time_width + 20, 21);

    m_timer = add<Core::Timer>(1000, [this] {
        static time_t last_update_time;
        time_t now = time(nullptr);
        if (now != last_update_time) {
            tick_clock();
            last_update_time = now;
            set_tooltip(Core::DateTime::now().to_string("%Y-%m-%d"));
        }
    });

    m_calendar_window = add<GUI::Window>(window());
    m_calendar_window->resize(m_window_size.width(), m_window_size.height());
    m_calendar_window->set_frameless(true);
    m_calendar_window->set_resizable(false);
    m_calendar_window->set_minimizable(false);
    m_calendar_window->on_active_input_change = [this](bool is_active_input) {
        if (!is_active_input)
            close();
    };

    auto& root_container = m_calendar_window->set_main_widget<GUI::Frame>();
    root_container.set_fill_with_background_color(true);
    root_container.set_layout<GUI::VerticalBoxLayout>();
    root_container.layout()->set_margins({ 2, 0 });
    root_container.layout()->set_spacing(0);
    root_container.set_frame_shape(Gfx::FrameShape::Window);

    auto& navigation_container = root_container.add<GUI::Widget>();
    navigation_container.set_fixed_height(24);
    navigation_container.set_layout<GUI::HorizontalBoxLayout>();
    navigation_container.layout()->set_margins({ 2 });

    m_prev_date = navigation_container.add<GUI::Button>();
    m_prev_date->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_prev_date->set_fixed_size(24, 24);
    m_prev_date->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png").release_value_but_fixme_should_propagate_errors());
    m_prev_date->on_click = [&](auto) {
        unsigned view_month = m_calendar->view_month();
        unsigned view_year = m_calendar->view_year();
        if (m_calendar->mode() == GUI::Calendar::Month) {
            view_month--;
            if (m_calendar->view_month() == 1) {
                view_month = 12;
                view_year--;
            }
        } else {
            view_year--;
        }
        m_calendar->update_tiles(view_year, view_month);
        if (m_calendar->mode() == GUI::Calendar::Year)
            m_selected_calendar_button->set_text(m_calendar->formatted_date(GUI::Calendar::YearOnly));
        else
            m_selected_calendar_button->set_text(m_calendar->formatted_date());
    };

    m_selected_calendar_button = navigation_container.add<GUI::Button>();
    m_selected_calendar_button->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_selected_calendar_button->set_fixed_height(24);
    m_selected_calendar_button->on_click = [&](auto) {
        m_calendar->toggle_mode();
        if (m_calendar->mode() == GUI::Calendar::Year)
            m_selected_calendar_button->set_text(m_calendar->formatted_date(GUI::Calendar::YearOnly));
        else
            m_selected_calendar_button->set_text(m_calendar->formatted_date());
    };

    m_next_date = navigation_container.add<GUI::Button>();
    m_next_date->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_next_date->set_fixed_size(24, 24);
    m_next_date->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png").release_value_but_fixme_should_propagate_errors());
    m_next_date->on_click = [&](auto) {
        unsigned view_month = m_calendar->view_month();
        unsigned view_year = m_calendar->view_year();
        if (m_calendar->mode() == GUI::Calendar::Month) {
            view_month++;
            if (m_calendar->view_month() == 12) {
                view_month = 1;
                view_year++;
            }
        } else {
            view_year++;
        }
        m_calendar->update_tiles(view_year, view_month);
        if (m_calendar->mode() == GUI::Calendar::Year)
            m_selected_calendar_button->set_text(m_calendar->formatted_date(GUI::Calendar::YearOnly));
        else
            m_selected_calendar_button->set_text(m_calendar->formatted_date());
    };

    auto& separator1 = root_container.add<GUI::HorizontalSeparator>();
    separator1.set_fixed_height(2);

    auto& calendar_container = root_container.add<GUI::Widget>();
    calendar_container.set_layout<GUI::HorizontalBoxLayout>();
    calendar_container.layout()->set_margins({ 2 });

    m_calendar = calendar_container.add<GUI::Calendar>();
    m_selected_calendar_button->set_text(m_calendar->formatted_date());

    m_calendar->on_tile_click = [&] {
        m_selected_calendar_button->set_text(m_calendar->formatted_date());
    };

    m_calendar->on_month_click = [&] {
        m_selected_calendar_button->set_text(m_calendar->formatted_date());
    };

    auto& separator2 = root_container.add<GUI::HorizontalSeparator>();
    separator2.set_fixed_height(2);

    auto& settings_container = root_container.add<GUI::Widget>();
    settings_container.set_fixed_height(24);
    settings_container.set_layout<GUI::HorizontalBoxLayout>();
    settings_container.layout()->set_margins({ 2 });
    settings_container.layout()->add_spacer();

    m_jump_to_button = settings_container.add<GUI::Button>();
    m_jump_to_button->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_jump_to_button->set_fixed_size(24, 24);
    m_jump_to_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/calendar-date.png").release_value_but_fixme_should_propagate_errors());
    m_jump_to_button->set_tooltip("Jump to today");
    m_jump_to_button->on_click = [this](auto) {
        jump_to_current_date();
    };

    m_calendar_launcher = settings_container.add<GUI::Button>();
    m_calendar_launcher->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_calendar_launcher->set_fixed_size(24, 24);
    m_calendar_launcher->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-calendar.png").release_value_but_fixme_should_propagate_errors());
    m_calendar_launcher->set_tooltip("Calendar");
    m_calendar_launcher->on_click = [](auto) {
        Core::Process::spawn("/bin/Calendar"sv);
    };
}

ClockWidget::~ClockWidget()
{
}

void ClockWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    auto time_text = Core::DateTime::now().to_string("%T");
    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());

    // Render string center-left aligned, but attempt to center the string based on a constant
    // "ideal" time string (i.e., the same one used to size this widget in the initializer).
    // This prevents the rest of the string from shifting around while seconds tick.
    const Gfx::Font& font = Gfx::FontDatabase::default_font();
    const int frame_width = frame_thickness();
    const int ideal_width = m_time_width;
    const int widget_width = max_width();
    const int translation_x = (widget_width - ideal_width) / 2 - frame_width;

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
        screen_relative_rect().right() - m_calendar_window->width() + 1,
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
    m_selected_calendar_button->set_text(m_calendar->formatted_date());
}

}
