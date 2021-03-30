/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include "ClockWidget.h"
#include <LibGUI/Painter.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <serenity.h>
#include <spawn.h>

namespace Taskbar {

ClockWidget::ClockWidget()
{
    set_frame_shape(Gfx::FrameShape::Box);
    set_frame_shadow(Gfx::FrameShadow::Sunken);
    set_frame_thickness(1);

    m_time_width = font().width("22:22:22");

    set_fixed_size(m_time_width + 20, 22);

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
    m_calendar_window->resize(152, 186);
    m_calendar_window->set_frameless(true);
    m_calendar_window->set_resizable(false);
    m_calendar_window->set_minimizable(false);
    m_calendar_window->on_active_input_change = [this](bool is_active_input) {
        if (!is_active_input)
            close();
    };

    auto& root_container = m_calendar_window->set_main_widget<GUI::Label>();
    root_container.set_fill_with_background_color(true);
    root_container.set_layout<GUI::VerticalBoxLayout>();
    root_container.layout()->set_margins({ 0, 2, 0, 2 });
    root_container.layout()->set_spacing(0);
    root_container.set_frame_thickness(2);
    root_container.set_frame_shape(Gfx::FrameShape::Container);
    root_container.set_frame_shadow(Gfx::FrameShadow::Raised);

    auto& navigation_container = root_container.add<GUI::Widget>();
    navigation_container.set_fixed_height(24);
    navigation_container.set_layout<GUI::HorizontalBoxLayout>();
    navigation_container.layout()->set_margins({ 2, 2, 3, 2 });

    m_prev_date = navigation_container.add<GUI::Button>();
    m_prev_date->set_button_style(Gfx::ButtonStyle::CoolBar);
    m_prev_date->set_fixed_size(24, 24);
    m_prev_date->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"));
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
    m_selected_calendar_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    m_selected_calendar_button->set_fixed_height(24);
    m_selected_calendar_button->on_click = [&](auto) {
        m_calendar->toggle_mode();
        if (m_calendar->mode() == GUI::Calendar::Year)
            m_selected_calendar_button->set_text(m_calendar->formatted_date(GUI::Calendar::YearOnly));
        else
            m_selected_calendar_button->set_text(m_calendar->formatted_date());
    };

    m_next_date = navigation_container.add<GUI::Button>();
    m_next_date->set_button_style(Gfx::ButtonStyle::CoolBar);
    m_next_date->set_fixed_size(24, 24);
    m_next_date->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"));
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

    auto& calendar_frame_container = root_container.add<GUI::Widget>();
    calendar_frame_container.set_layout<GUI::HorizontalBoxLayout>();
    calendar_frame_container.layout()->set_margins({ 4, 4, 5, 4 });

    auto& calendar_frame = calendar_frame_container.add<GUI::Frame>();
    calendar_frame.set_layout<GUI::VerticalBoxLayout>();
    calendar_frame.layout()->set_margins({ 2, 2, 2, 2 });

    m_calendar = calendar_frame.add<GUI::Calendar>();
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
    settings_container.layout()->set_margins({ 2, 2, 3, 2 });
    settings_container.layout()->add_spacer();

    m_jump_to_button = settings_container.add<GUI::Button>();
    m_jump_to_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    m_jump_to_button->set_fixed_size(24, 24);
    m_jump_to_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/calendar-date.png"));
    m_jump_to_button->set_tooltip("Jump to today");
    m_jump_to_button->on_click = [this](auto) {
        jump_to_current_date();
    };

    m_calendar_launcher = settings_container.add<GUI::Button>();
    m_calendar_launcher->set_button_style(Gfx::ButtonStyle::CoolBar);
    m_calendar_launcher->set_fixed_size(24, 24);
    m_calendar_launcher->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-calendar.png"));
    m_calendar_launcher->set_tooltip("Calendar");
    m_calendar_launcher->on_click = [](auto) {
        pid_t pid;
        const char* argv[] = { "Calendar", nullptr };
        if ((errno = posix_spawn(&pid, "/bin/Calendar", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(pid) < 0)
                perror("disown");
        }
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
    painter.draw_text(event.rect(), time_text, Gfx::FontDatabase::default_font(), Gfx::TextAlignment::Center, palette().window_text());
}

void ClockWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Left) {
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
    m_calendar_window->set_rect(
        screen_relative_rect().right() - m_calendar_window->width() + 4,
        screen_relative_rect().top() - m_calendar_window->height() - 3,
        152,
        186);
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
